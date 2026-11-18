const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validVueltaPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    ownerId: uid,
    ownerName: "Ana Pérez",
    memberUids: [uid],
    title: "Pagar Internet",
    templateKey: "internet",
    notes: "Plan fibra Altice",
    dueDateMillis: Date.now() + 86400000,
    remind2Days: true,
    remind1Day: true,
    remindSameDay: true,
    reminderHour: 9,
    reminderMinute: 0,
    completed: false,
    sharedWithFamily: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write users, vueltas, families, or invites", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("vueltas").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauth.collection("vueltas").doc("v1").set(validVueltaPayload(ALICE_UID)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("families").doc("fam1").get());
  await assertFails(unauth.collection("family_invites").doc("MV1234").get());
});

test("User profile: owner CRUD succeeds, cross-user access and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ana Pérez",
      email: "ana@example.com",
      planTier: "free",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update to Pro plan succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "pro",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Vueltas: owner create, filtered query, partial complete, delete succeed; unfiltered & @DocumentId id fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Create valid vuelta without 'id' field
  await assertSucceeds(
    aliceDb.collection("vueltas").doc("vuelta_1").set(validVueltaPayload(ALICE_UID))
  );

  // Reject payload containing 'id' field (protects @DocumentId)
  await assertFails(
    aliceDb.collection("vueltas").doc("vuelta_2").set(
      validVueltaPayload(ALICE_UID, { id: "vuelta_2" })
    )
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb.collection("vueltas").doc("vuelta_3").set(
      validVueltaPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    aliceDb.collection("vueltas").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Unfiltered query fails
  await assertFails(aliceDb.collection("vueltas").get());

  // Cross-user read & write fail when Bob is not in memberUids
  await assertFails(bobDb.collection("vueltas").doc("vuelta_1").get());
  await assertFails(
    bobDb.collection("vueltas").doc("vuelta_1").update({
      completed: true,
      completedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner marks vuelta completed
  await assertSucceeds(
    aliceDb.collection("vueltas").doc("vuelta_1").update({
      completed: true,
      completedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable ownerId or createdAt tamper
  await assertFails(
    aliceDb.collection("vueltas").doc("vuelta_1").update({
      ownerId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner deletes vuelta
  await assertSucceeds(aliceDb.collection("vueltas").doc("vuelta_1").delete());
});

test("Family sharing: member can toggle completion on shared vuelta, non-owner cannot delete; invite lookup works without list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // Alice creates family and invite
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_alice").set({
      ownerId: ALICE_UID,
      name: "Familia Pérez",
      inviteCode: "MV4829",
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("family_invites").doc("MV4829").set({
      familyId: "fam_alice",
      createdBy: ALICE_UID,
      createdAt: now,
    })
  );

  // Listing invites must fail
  await assertFails(bobDb.collection("family_invites").get());

  // Bob looks up exact invite code & joins family
  await assertSucceeds(bobDb.collection("family_invites").doc("MV4829").get());
  await assertSucceeds(
    bobDb.collection("families").doc("fam_alice").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice creates shared vuelta including Bob
  await assertSucceeds(
    aliceDb.collection("vueltas").doc("shared_1").set(
      validVueltaPayload(ALICE_UID, {
        memberUids: [ALICE_UID, BOB_UID],
        sharedWithFamily: true,
      })
    )
  );

  // Bob can query and mark shared vuelta completed
  await assertSucceeds(
    bobDb.collection("vueltas").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("vueltas").doc("shared_1").update({
      completed: true,
      completedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot edit title or delete Alice's vuelta
  await assertFails(
    bobDb.collection("vueltas").doc("shared_1").update({
      title: "Cambiado por Bob",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("vueltas").doc("shared_1").delete());

  // Charlie cannot read shared vuelta
  await assertFails(charlieDb.collection("vueltas").doc("shared_1").get());
});
