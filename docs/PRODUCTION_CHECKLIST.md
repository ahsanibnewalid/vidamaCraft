# Production readiness checklist

VidamaCraft is an early MVP scaffold, not a production release. Do not publish an APK until these gates are completed.

## Product and compatibility
- [ ] Confirm supported Android versions and test on physical devices.
- [ ] Add persistent download history and reliable status updates from DownloadManager.
- [ ] Handle duplicate filenames, expired links, redirects, large files, low storage, and network loss.
- [ ] Add cancellation, retry, pause/resume behavior where supported, and accessible error states.
- [ ] Test URLs from trusted sources; URL validation does not grant permission or confirm media type.

## Security and privacy
- [ ] Threat-model URL handling, redirects, filename/path handling, and untrusted content.
- [ ] Review Android network security configuration and exported components.
- [ ] Add privacy policy and clear data-retention disclosures.
- [ ] Confirm no DRM circumvention, credential harvesting, or access-control bypass.
- [ ] Run dependency and static analysis checks and remediate high-severity findings.

## Quality and release
- [ ] Add UI/instrumentation tests and tests for download failures.
- [ ] Run CI on pull requests and protect `main`.
- [ ] Verify release build, minification, crash behavior, and accessibility.
- [ ] Configure signing securely outside source control; never commit keystores or passwords.
- [ ] Prepare release notes, license notices, store policy review, and support contact.
- [ ] Build and verify a release APK/AAB on supported devices before claiming production readiness.
