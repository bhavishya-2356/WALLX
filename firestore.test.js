const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const OWNER_UID = "owner_uid_999";
const OWNER_EMAIL = "panchalrahul18827@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read, list, or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauthorized",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner: Kotlin create, get, and partial update payloads succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Panchal Rahul",
      email: "panchal@example.com",
      photoUrl: "https://lh3.googleusercontent.com/a/example",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Panchal Rahul Updated",
      photoUrl: "https://lh3.googleusercontent.com/a/updated",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).delete()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "WALLX Member",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("PII Isolation & Cross-User Security: user cannot read, list, or mutate another user's profile", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).delete());
});

test("Security Bounds: rejects ghost fields, immutable mutation, identity spoofing, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID,
      displayName: "Alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "A".repeat(101),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("AppRelease /app_config/latest_release: owner can publish, signed-in users can check, non-owners and unauth cannot write", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const userDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();
  const unverifiedOwnerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: false })
    .firestore();
  const now = new Date(Date.now() - 1000);

  // Unauthenticated cannot read or write app_config
  await assertFails(unauthDb.collection("app_config").doc("latest_release").get());
  await assertFails(
    unauthDb.collection("app_config").doc("latest_release").set({
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "Unauthorized release",
      forceUpdate: false,
      updatedBy: ALICE_UID,
      updatedAt: now,
    })
  );

  // Regular signed-in user cannot publish or delete updates
  await assertFails(
    userDb.collection("app_config").doc("latest_release").set({
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "Hijack attempt",
      forceUpdate: false,
      updatedBy: ALICE_UID,
      updatedAt: now,
    })
  );

  // Unverified owner token cannot publish
  await assertFails(
    unverifiedOwnerDb.collection("app_config").doc("latest_release").set({
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "Unverified attempt",
      forceUpdate: false,
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );

  // Verified owner publishes valid release matching AppUpdateRepository Kotlin payload
  await assertSucceeds(
    ownerDb.collection("app_config").doc("latest_release").set({
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "New 16K wallpapers & smoother scrolling",
      downloadUrl: "https://example.com/wallx-1.1.apk",
      forceUpdate: false,
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );

  // Signed-in regular user can read latest_release directly (auto-check & manual check)
  await assertSucceeds(userDb.collection("app_config").doc("latest_release").get());
  // Listing collection or reading arbitrary doc IDs must fail
  await assertFails(userDb.collection("app_config").get());
  await assertFails(userDb.collection("app_config").doc("other_doc").get());

  // Owner can update without optional downloadUrl
  await assertSucceeds(
    ownerDb.collection("app_config").doc("latest_release").set({
      versionCode: 3,
      versionName: "1.2",
      releaseNotes: "Performance boost",
      forceUpdate: true,
      updatedBy: OWNER_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner cannot delete release doc, spoof updatedBy, or pass invalid versionCode/future time
  await assertFails(ownerDb.collection("app_config").doc("latest_release").delete());
  await assertFails(
    ownerDb.collection("app_config").doc("latest_release").set({
      versionCode: 0,
      versionName: "1.2",
      releaseNotes: "Invalid version code",
      forceUpdate: false,
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );
  await assertFails(
    ownerDb.collection("app_config").doc("latest_release").set({
      versionCode: 4,
      versionName: "1.3",
      releaseNotes: "Future timestamp",
      forceUpdate: false,
      updatedBy: OWNER_UID,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
