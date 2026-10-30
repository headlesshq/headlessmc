# Changelog

## [3.0.0-RC9](https://github.com/headlesshq/headlessmc/compare/3.0.0-RC8...3.0.0-RC9) (2026-10-30)


### Features

* **auth:** implement the Yggdrasil auth provider ([#486](https://github.com/headlesshq/headlessmc/issues/486)) ([c83e908](https://github.com/headlesshq/headlessmc/commit/c83e908335e0a23552a273143a219ec5600b9be9))
* **commands:** made login and install/download separate hidden commands ([#479](https://github.com/headlesshq/headlessmc/issues/479)) ([e29809e](https://github.com/headlesshq/headlessmc/commit/e29809edea8f201ee57d06a2ec8e247abddb0703))
* **curseforge:** add CurseForge mod distribution platform ([#505](https://github.com/headlesshq/headlessmc/issues/505)) ([ee1497f](https://github.com/headlesshq/headlessmc/commit/ee1497f08ce7d02950985b0b24665250255e8349))
* **i18n:** add i18n support and a chinese translation ([#493](https://github.com/headlesshq/headlessmc/issues/493)) ([b1337a7](https://github.com/headlesshq/headlessmc/commit/b1337a74ae40decf7042bd3ce8c9a92a13684639))
* **java:** updater for java versions ([#502](https://github.com/headlesshq/headlessmc/issues/502)) ([3aac0ff](https://github.com/headlesshq/headlessmc/commit/3aac0ff3acf1181e22f0c85f511b8f397808b55f))
* **mods:** add GitHub mod distribution platform ([#506](https://github.com/headlesshq/headlessmc/issues/506)) ([0011d94](https://github.com/headlesshq/headlessmc/commit/0011d9470e666d6d74e9638e0c0936387e1c800e))
* **secrets:** censor secrets in console and log output ([#509](https://github.com/headlesshq/headlessmc/issues/509)) ([f4be494](https://github.com/headlesshq/headlessmc/commit/f4be49426e8fb0d5144e0613103e406743d5e33b))
* **updater:** notify user when new GitHub release is available ([#498](https://github.com/headlesshq/headlessmc/issues/498)) ([9d81ea3](https://github.com/headlesshq/headlessmc/commit/9d81ea3e9283680fa6a32630dc3b70c0ad3c8e7a))
* **version:** add version delete command ([#499](https://github.com/headlesshq/headlessmc/issues/499)) ([926c831](https://github.com/headlesshq/headlessmc/commit/926c831baf97d1bdcbed70b719ba357aea4c0c97))


### Bug Fixes

* **cdi:** make ApplicationScoped DummyProgressBarService non-final ([#497](https://github.com/headlesshq/headlessmc/issues/497)) ([baa9511](https://github.com/headlesshq/headlessmc/commit/baa951187ef2804b35a7a0c5062b043239a70bd9))
* **cdi:** remove final from JacksonVersionParser instance methods ([#500](https://github.com/headlesshq/headlessmc/issues/500)) ([8c6f9b8](https://github.com/headlesshq/headlessmc/commit/8c6f9b8488f4a67f295a40e072d16b0cdd516bad))
* **deps:** update asm to v9.11.1 ([#481](https://github.com/headlesshq/headlessmc/issues/481)) ([62bbfca](https://github.com/headlesshq/headlessmc/commit/62bbfca5256c6b8393154eb29a9f724c91aa8977))
* **deps:** update dependency com.google.code.gson:gson to v2.15.0 ([#494](https://github.com/headlesshq/headlessmc/issues/494)) ([4b29bda](https://github.com/headlesshq/headlessmc/commit/4b29bda6c18d961be3c3dea9c309bcb66ebf18a3))
* **deps:** update dependency io.quarkus to v3.40.1.1 ([#483](https://github.com/headlesshq/headlessmc/issues/483)) ([79dcdbd](https://github.com/headlesshq/headlessmc/commit/79dcdbda61335fa8066fe2d5a584a1edc15da866))
* **launch:** launch version.jsons with custom names ([#503](https://github.com/headlesshq/headlessmc/issues/503)) ([fae024c](https://github.com/headlesshq/headlessmc/commit/fae024c8aa080505581933b4263efaa9ea10f226))
* **launch:** support legacy --server argument ([#495](https://github.com/headlesshq/headlessmc/issues/495)) ([cdab3ef](https://github.com/headlesshq/headlessmc/commit/cdab3ef76d4eaa0c2aa3c01672878d03c4db8caa))
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
