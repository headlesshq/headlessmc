# Changelog

## [3.0.0-RC9](https://github.com/headlesshq/headlessmc/compare/3.0.0-RC8...3.0.0-RC9) (2026-11-14)


### Features

* add static musl executable for linux x64 and detect musl systems ([#547](https://github.com/headlesshq/headlessmc/issues/547)) ([5c24887](https://github.com/headlesshq/headlessmc/commit/5c2488741154fe5e327457b4cc26b234b927450e))
* **auth:** implement the Yggdrasil auth provider ([#486](https://github.com/headlesshq/headlessmc/issues/486)) ([c83e908](https://github.com/headlesshq/headlessmc/commit/c83e908335e0a23552a273143a219ec5600b9be9))
* **backup:** add backup command and BackupService ([#521](https://github.com/headlesshq/headlessmc/issues/521)) ([c05d422](https://github.com/headlesshq/headlessmc/commit/c05d4227f7a7b1a08b58767acc2f7477f91017b6))
* **commands:** made login and install/download separate hidden commands ([#479](https://github.com/headlesshq/headlessmc/issues/479)) ([e29809e](https://github.com/headlesshq/headlessmc/commit/e29809edea8f201ee57d06a2ec8e247abddb0703))
* **completions:** add shell completions ([#516](https://github.com/headlesshq/headlessmc/issues/516)) ([c6ee593](https://github.com/headlesshq/headlessmc/commit/c6ee593d8a36c168c9c4d984347c027ad7f816e0))
* **curseforge:** add CurseForge mod distribution platform ([#505](https://github.com/headlesshq/headlessmc/issues/505)) ([ee1497f](https://github.com/headlesshq/headlessmc/commit/ee1497f08ce7d02950985b0b24665250255e8349))
* **i18n:** add i18n support and a chinese translation ([#493](https://github.com/headlesshq/headlessmc/issues/493)) ([b1337a7](https://github.com/headlesshq/headlessmc/commit/b1337a74ae40decf7042bd3ce8c9a92a13684639))
* **java:** download jmods for Java distributions without them ([#536](https://github.com/headlesshq/headlessmc/issues/536)) ([f790d0a](https://github.com/headlesshq/headlessmc/commit/f790d0a141896b218322e53190b2b4bd2be97b3b))
* **java:** updater for java versions ([#502](https://github.com/headlesshq/headlessmc/issues/502)) ([3aac0ff](https://github.com/headlesshq/headlessmc/commit/3aac0ff3acf1181e22f0c85f511b8f397808b55f))
* **jline:** make grapheme cluster detection configurable ([#517](https://github.com/headlesshq/headlessmc/issues/517)) ([b57a9c9](https://github.com/headlesshq/headlessmc/commit/b57a9c9fe9e118db2c1b918186aad93d5907a119))
* **json:** relative paths, allow users to move headlessmc files between machines ([#510](https://github.com/headlesshq/headlessmc/issues/510)) ([8a1e2bb](https://github.com/headlesshq/headlessmc/commit/8a1e2bbd60eff869da8f6cde836023acacf9916e))
* **launch:** add --dry-run to download everything without launching ([#554](https://github.com/headlesshq/headlessmc/issues/554)) ([2553e36](https://github.com/headlesshq/headlessmc/commit/2553e366bf2710f51eaf1631983be2f5814c7e30))
* **lwjgl:** add lwjgl-pre patcher, which uses headlessmc-lwjgl ([#518](https://github.com/headlesshq/headlessmc/issues/518)) ([fa2ca90](https://github.com/headlesshq/headlessmc/commit/fa2ca90b8ab2d9ce874f58f19f2e7ccd610d3dca))
* **mods:** add GitHub mod distribution platform ([#506](https://github.com/headlesshq/headlessmc/issues/506)) ([0011d94](https://github.com/headlesshq/headlessmc/commit/0011d9470e666d6d74e9638e0c0936387e1c800e))
* **plugins:** load plugins by re-augmenting a mutable jar ([#520](https://github.com/headlesshq/headlessmc/issues/520)) ([63f9fe4](https://github.com/headlesshq/headlessmc/commit/63f9fe4e23b3af198047fc14eb8b60dc36a52cba))
* **secrets:** censor secrets in console and log output ([#509](https://github.com/headlesshq/headlessmc/issues/509)) ([f4be494](https://github.com/headlesshq/headlessmc/commit/f4be49426e8fb0d5144e0613103e406743d5e33b))
* **server:** default server jvm args from Paper's recommended flags ([#512](https://github.com/headlesshq/headlessmc/issues/512)) ([dd4de96](https://github.com/headlesshq/headlessmc/commit/dd4de96e91f01f77ad2d4248b50cccd49b36d91b))
* **updater:** notify user when new GitHub release is available ([#498](https://github.com/headlesshq/headlessmc/issues/498)) ([9d81ea3](https://github.com/headlesshq/headlessmc/commit/9d81ea3e9283680fa6a32630dc3b70c0ad3c8e7a))
* **version:** add stable classifier to VersionArg ([#522](https://github.com/headlesshq/headlessmc/issues/522)) ([b301315](https://github.com/headlesshq/headlessmc/commit/b30131576126266c454713ac0390d57208613507))
* **version:** add version delete command ([#499](https://github.com/headlesshq/headlessmc/issues/499)) ([926c831](https://github.com/headlesshq/headlessmc/commit/926c831baf97d1bdcbed70b719ba357aea4c0c97))


### Bug Fixes

* **cdi:** make ApplicationScoped DummyProgressBarService non-final ([#497](https://github.com/headlesshq/headlessmc/issues/497)) ([baa9511](https://github.com/headlesshq/headlessmc/commit/baa951187ef2804b35a7a0c5062b043239a70bd9))
* **cdi:** remove final from JacksonVersionParser instance methods ([#500](https://github.com/headlesshq/headlessmc/issues/500)) ([8c6f9b8](https://github.com/headlesshq/headlessmc/commit/8c6f9b8488f4a67f295a40e072d16b0cdd516bad))
* **deps:** update asm to v9.11.1 ([#481](https://github.com/headlesshq/headlessmc/issues/481)) ([62bbfca](https://github.com/headlesshq/headlessmc/commit/62bbfca5256c6b8393154eb29a9f724c91aa8977))
* **deps:** update dependency com.google.code.gson:gson to v2.15.0 ([#494](https://github.com/headlesshq/headlessmc/issues/494)) ([4b29bda](https://github.com/headlesshq/headlessmc/commit/4b29bda6c18d961be3c3dea9c309bcb66ebf18a3))
* **deps:** update dependency com.google.jimfs:jimfs to v1.3.2 ([#541](https://github.com/headlesshq/headlessmc/issues/541)) ([138eba3](https://github.com/headlesshq/headlessmc/commit/138eba32a18adfc5a25160a405fa339374c20980))
* **deps:** update dependency io.quarkus to v3.40.1.1 ([#483](https://github.com/headlesshq/headlessmc/issues/483)) ([79dcdbd](https://github.com/headlesshq/headlessmc/commit/79dcdbda61335fa8066fe2d5a584a1edc15da866))
* **deps:** update dependency org.apache.commons:commons-lang3 to v3.22.0 ([#564](https://github.com/headlesshq/headlessmc/issues/564)) ([a50104f](https://github.com/headlesshq/headlessmc/commit/a50104f3510cfc15ac78121178ef38c11b889ac8))
* **deps:** update dependency org.jline:jline to v4.5.0 ([#525](https://github.com/headlesshq/headlessmc/issues/525)) ([e1de5d6](https://github.com/headlesshq/headlessmc/commit/e1de5d635408268ea5a0cb179509320510fea31f))
* **deps:** update jline to v4.6.0 ([#559](https://github.com/headlesshq/headlessmc/issues/559)) ([581d7cd](https://github.com/headlesshq/headlessmc/commit/581d7cd3053fab590543f6405347326ede7fb4b1))
* **deps:** update jline to v4.7.0 ([#568](https://github.com/headlesshq/headlessmc/issues/568)) ([b7d063d](https://github.com/headlesshq/headlessmc/commit/b7d063d51573a7695e9538b934cfe996025740c0))
* **deps:** update plugin versions to v0.67.0 ([#524](https://github.com/headlesshq/headlessmc/issues/524)) ([a6f363f](https://github.com/headlesshq/headlessmc/commit/a6f363f021341cff0e22ecded263025137625509))
* **deps:** update quarkus to v3.40.1.2 ([#556](https://github.com/headlesshq/headlessmc/issues/556)) ([0f3e1aa](https://github.com/headlesshq/headlessmc/commit/0f3e1aa03e238de7421b5f1f25d473f7b558f0f0))
* **java:** make foojay java list/install work in native images ([#548](https://github.com/headlesshq/headlessmc/issues/548)) ([0fa4a1e](https://github.com/headlesshq/headlessmc/commit/0fa4a1ee253aca4f9ac8187d939bc33a1bc80f3a))
* **launch:** launch version.jsons with custom names ([#503](https://github.com/headlesshq/headlessmc/issues/503)) ([fae024c](https://github.com/headlesshq/headlessmc/commit/fae024c8aa080505581933b4263efaa9ea10f226))
* **launch:** support legacy --server argument ([#495](https://github.com/headlesshq/headlessmc/issues/495)) ([cdab3ef](https://github.com/headlesshq/headlessmc/commit/cdab3ef76d4eaa0c2aa3c01672878d03c4db8caa))
* **platform:** match rewritten version on forced reinstall ([#553](https://github.com/headlesshq/headlessmc/issues/553)) ([34617c3](https://github.com/headlesshq/headlessmc/commit/34617c30c76d43a60f8e0497818b6aef936aa0ab))
* **profile:** bump profile version for relative path format ([#532](https://github.com/headlesshq/headlessmc/issues/532)) ([c93e385](https://github.com/headlesshq/headlessmc/commit/c93e3858420d6af23eb16b0fe423dc9f3c70f699))
* **progressbar:** no more conflicts with open Jline terminal ([#519](https://github.com/headlesshq/headlessmc/issues/519)) ([619702b](https://github.com/headlesshq/headlessmc/commit/619702bd69d32aee62824e7cc820213ca784a835))
* **progressbar:** no more conflicts with open Jline terminal ([#552](https://github.com/headlesshq/headlessmc/issues/552)) ([e8e4211](https://github.com/headlesshq/headlessmc/commit/e8e4211d9befc5f99787cadac4a71ee246e7945f))
* **test:** fix test WAIT timeout ([#476](https://github.com/headlesshq/headlessmc/issues/476)) ([00cd6fb](https://github.com/headlesshq/headlessmc/commit/00cd6fb49f8755d4db162e3900bde3bb54d4cb38))

## [3.0.0-RC8](https://github.com/headlesshq/headlessmc/compare/3.0.0-RC7...3.0.0-RC8) (2026-10-15)


### Features

* **specifics:** pass specifics to game system properties ([#470](https://github.com/headlesshq/headlessmc/issues/470)) ([2fe59a7](https://github.com/headlesshq/headlessmc/commit/2fe59a70f73407e1098c4520e0702a0f0c22cfd6))


### Bug Fixes

* **lwjgl:** relocate ASM in agent jar and report transform failures ([#473](https://github.com/headlesshq/headlessmc/issues/473)) ([aa14b29](https://github.com/headlesshq/headlessmc/commit/aa14b293b56cfb437a3a37fec61c62c47dab90ec)), closes [#471](https://github.com/headlesshq/headlessmc/issues/471) [#472](https://github.com/headlesshq/headlessmc/issues/472)
* **net:** delete files when hash verification failed ([#474](https://github.com/headlesshq/headlessmc/issues/474)) ([544db67](https://github.com/headlesshq/headlessmc/commit/544db67fa2984f7e6e71f18bbe9775d8947d2e6a))
* **windows:** fixed line ending and patching issues ([#466](https://github.com/headlesshq/headlessmc/issues/466)) ([5b16cbf](https://github.com/headlesshq/headlessmc/commit/5b16cbf6a00ec75ba0dedd31b2e8aa6eb103a204))

## [3.0.0-RC7](https://github.com/headlesshq/headlessmc/compare/3.0.0-RC6...3.0.0-RC7) (2026-10-13)


### Bug Fixes

* **deps:** update asm to v9.11 ([#461](https://github.com/headlesshq/headlessmc/issues/461)) ([983b648](https://github.com/headlesshq/headlessmc/commit/983b6486a2213399c2fea0b2b010ca610332088a))
* **deps:** update dependency net.fabricmc:fabric-installer to v1.1.2 ([#459](https://github.com/headlesshq/headlessmc/issues/459)) ([ce550e4](https://github.com/headlesshq/headlessmc/commit/ce550e40649a55a0c1127f97d2a19da4a7312304))
* **deps:** update jline to v4.4.8 ([#460](https://github.com/headlesshq/headlessmc/issues/460)) ([a3f1385](https://github.com/headlesshq/headlessmc/commit/a3f138578f09ee0a6a5fa49d5d142bf6b94842a6))
* **deps:** update plugin versions to v0.66.0 ([#463](https://github.com/headlesshq/headlessmc/issues/463)) ([37fbe9c](https://github.com/headlesshq/headlessmc/commit/37fbe9cc9c15f4d185bb07411a71f90f549355e7))
* **server:** fix server testing and full std buffers on windows ([#453](https://github.com/headlesshq/headlessmc/issues/453)) ([53137b0](https://github.com/headlesshq/headlessmc/commit/53137b0b55668f430aca8abed06a7c22f330738f))

## [3.0.0-RC6](https://github.com/headlesshq/headlessmc/compare/3.0.0-RC5...3.0.0-RC6) (2026-10-12)


### Features

* **server:** launch forge and neoforge run.sh/run.bat scripts ([#450](https://github.com/headlesshq/headlessmc/issues/450)) ([9061cd7](https://github.com/headlesshq/headlessmc/commit/9061cd79273fab4ca538938724c9b1865cb041db))
