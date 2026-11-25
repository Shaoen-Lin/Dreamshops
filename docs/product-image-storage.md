# Product image storage rollout

The storefront still renders `GET /api/v1/images/image/download/{imageID}`. Previously the API wrote and read PostgreSQL large objects through the image row. Image replacement overwrote the row's BLOB reference, and image or product deletion removed image rows. The new path writes private objects and keeps image IDs, URLs, and metadata in PostgreSQL. Legacy rows with no object key continue to read their original BLOBs. The November 25, 2026 read-only inventory found two Cloud Build buckets, no dedicated product-image bucket, and Cloud SQL stopped with activation policy `NEVER`. The existing image count and bytes remain unverified.

## Approval required before running cloud commands

1. Approve creation of a Standard Storage bucket in `asia-east1`, and a bucket-level object grant to the existing backend runtime service account. The proposed name returned 404 during inspection. Bucket names are globally unique, so confirm availability when provisioning. Use a verified private dedicated bucket if one is available then.
2. Set the GitHub repository variable `SHOP_IMAGES_BUCKET` to the approved bucket name. Approve dispatching [Dreamshop CD](../.github/workflows/cd.yml) from a commit with successful CI. This publishes both images and stages two Cloud Run revisions at zero traffic. Workload Identity Federation, the current runtime service account, and the backend secret references are retained.
3. Approve a separate environment with a disposable database and private bucket for smoke tests, or approve a bounded window to use the existing database. The workflow itself performs no traffic change. A staged backend using the stopped database cannot pass application smoke tests. Any traffic promotion or Cloud SQL activation requires a separate decision.

The commands below are the proposed provisioning steps.

```bash
project=pinbot-devops
bucket=pinbot-devops-dreamshops-product-images
runtime_account=dreamshops-backend@pinbot-devops.iam.gserviceaccount.com

gcloud storage buckets create "gs://$bucket" \
  --project="$project" --location=asia-east1 --default-storage-class=STANDARD \
  --uniform-bucket-level-access --public-access-prevention

gcloud storage buckets add-iam-policy-binding "gs://$bucket" \
  --member="serviceAccount:$runtime_account" --role=roles/storage.objectUser

gh variable set SHOP_IMAGES_BUCKET --repo Shaoen-Lin/Dreamshops --body "$bucket"
```

The [predefined Storage Object User role](https://docs.cloud.google.com/storage/docs/access-control/iam-roles) permits object operations without bucket administration. Scope the grant to the dedicated bucket. Verify [uniform bucket-level access](https://docs.cloud.google.com/storage/docs/uniform-bucket-level-access), [public access prevention](https://docs.cloud.google.com/storage/docs/public-access-prevention), and the IAM policy before deployment. The backend uses ADC from its Cloud Run identity. Keep service-account key files out of the build and runtime.

For approved zero-traffic smoke tests, use the staging revision URL through a temporary revision tag or another authorized staging endpoint. Check Cloud Run readiness, admin upload and replacement, anonymous image viewing through the API, denied image writes for a customer, metadata-only rows for fresh images, a legacy BLOB read, and eventual removal of an unreferenced test object. Verify catalog, cart, checkout, and order history. Keep the bucket private and test with disposable images. The GitHub workflow creates the revisions; this smoke test and any temporary revision routing are separate operator actions.

## Legacy migration and rollback

1. During an approved window, take a recoverable database backup that includes PostgreSQL large objects, and test the restore. Inventory image rows by object key and BLOB presence without modifying them. The application's existing Hibernate schema update adds nullable `object_key`, `object_store`, and `byte_size` columns and an `image_object_cleanup` table. Staging should verify this upgrade against a representative legacy database. Keep the original `image` column and large objects.
2. If a later backfill is authorized, work in bounded batches. For each legacy row, lock it and recheck that its object key is null, read its BLOB, create a new private object without overwrite, and verify size and a checksum against the original. Commit only its storage metadata and retain its original bytes. Preserve the existing image ID, URL, MIME type, and product link. A retried batch skips rows with committed keys. No backfill is performed by this release.
3. Verify each migrated row and the public download end to end before considering BLOB retirement in a separately approved change. After new object-backed rows exist, use a storage-aware revision with the same bucket for application rollback. Before redirecting to a pre-GCS binary, pause image mutations and restore or rehydrate current image bytes into BLOBs in a verified migration. Older binaries cannot read new object-backed rows, and retained legacy bytes can predate a replacement.
4. A database commit queues old object deletion for replacements, image deletes, and product deletes. A scheduled worker retries failed removals while instances run. Cloud Run can scale to zero, so cleanup has no guaranteed wall-clock deadline. Monitor outstanding rows during rollout. Confirmed rollback attempts to delete freshly uploaded bytes. A crash, ambiguous storage write, unknown DB commit outcome, or failed rollback cleanup can leave an orphan. Reconcile aged objects against both committed image metadata and the removal queue after checking for in-flight writes. Use a conservative age window and review candidates before any deletion. Avoid an age-based lifecycle rule on the live image prefix, which also contains active images.
