# Legal Basis for Essential Patcher

**Last updated:** May 27, 2026

This document explains the legal and technical position of Essential Patcher. It is written for repository readers, users, maintainers, and platform reviewers. It is not legal advice.

Essential Patcher is not affiliated with, endorsed by, approved by, or associated with Essential, ModCore Inc., Spark Universe, Mojang, Microsoft, or Minecraft.

## Short Position

Essential Patcher is a free, independently written Minecraft: Java Edition mod. It patches the local runtime behavior of the Essential mod on the user's own client.

The project position is that Essential Patcher is lawful and defensible because it:

- distributes original patcher code only;
- does not distribute Essential's JAR, a modified Essential JAR, Essential assets, Minecraft's JAR, a modified Minecraft JAR, or Minecraft assets;
- runs locally on a client controlled by the user;
- does not alter Essential's servers, purchase records, account entitlements, or payment systems;
- does not send forged purchase, checkout, coin, subscription, ownership, or entitlement data to Essential;
- uses an independent HTTPS sync service only for patcher-to-patcher cosmetic display;
- is free, non-commercial, and open source;
- exists for interoperability, privacy, user control, and local client-side customization.

There can still be contractual or platform risk. Essential, Mojang, Microsoft, a hosting provider, or a court may disagree with this project's position. This document does not guarantee any outcome.

## Sources Reviewed

This document relies on the following public sources:

- Essential Terms and Conditions of Use, last updated November 22, 2024: https://essential.gg/terms-of-use
- Essential Privacy Policy, last updated October 28, 2024: https://essential.gg/privacy-policy
- Minecraft French EULA / CLUF: https://www.minecraft.net/fr-fr/eula
- Directive 2009/24/EC on the legal protection of computer programs: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32009L0024
- Regulation (EU) 2016/679, General Data Protection Regulation: https://eur-lex.europa.eu/eli/reg/2016/679/oj
- Directive 2005/29/EC on unfair business-to-consumer commercial practices: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32005L0029
- Regulation (EU) 2022/2065, Digital Services Act: https://eur-lex.europa.eu/eli/reg/2022/2065/oj/eng
- French Intellectual Property Code, Article L122-6-1: https://www.legifrance.gouv.fr/codes/article_lc/LEGIARTI000044365559
- French Consumer Code, Article L212-1: https://www.legifrance.gouv.fr/codes/article_lc/LEGIARTI000032890812

## What Essential Patcher Is

Essential Patcher is a compatibility and user-control mod. It uses Mixins to modify local client behavior at runtime. The repository contains patcher source code, build files, mod metadata, and documentation.

It does not contain:

- Essential source code;
- Essential decompiled source files;
- Essential models, textures, sounds, icons, cosmetics, trade dress, or branding;
- Minecraft source code;
- Minecraft assets;
- a modified Minecraft client or server;
- a modified Essential binary;
- payment credentials, API secrets, session tokens, or server credentials.

Essential Patcher does not claim that users own Essential cosmetics they have not purchased. It does not claim to grant official Essential entitlements. Its cosmetic behavior is local visual customization and opt-in sync between patcher users.

## What Essential's Own Documents Say

Essential's Terms state that the service includes Essential's Minecraft mods, Coins, Digital Goods such as cosmetics and emotes, the Store, and subscription services. They also state that Essential's license is limited, personal, revocable, non-transferable, non-assignable, and non-sublicensable.

The purchase terms are important. Essential says Coins are virtual currency used only in the Store, have no real-world monetary value, and cannot be transferred or exchanged. Essential says Digital Goods are licensed, not owned, and that redeeming Digital Goods does not transfer ownership. Essential also says access to digital goods may be revoked or lost in some circumstances, including if Essential no longer operates or maintains the service, store, or particular digital goods.

Essential's Terms also prohibit decompiling, reverse engineering, or disassembling software or products accessible through the service. This project does not ignore that clause. The project position is that contractual terms cannot remove mandatory statutory rights that apply in the user's jurisdiction, including EU and French software interoperability rights discussed below.

Essential's Privacy Policy says Essential may collect account information, email address, Minecraft UUID, usage times and dates, time zone, language, screen resolution, device settings, purchased items, cosmetic configurations, Minecraft version, mod loader version, concurrent mods, browser type, operating system, IP address, referring and exit pages, unique identifiers, push tokens, activity linked to email or Minecraft UUID, metadata, support information, third-party information, and geolocation at country/state/territory level.

The Privacy Policy also states that Essential may process EU user data under GDPR Article 6(1)(a), 6(1)(b), 6(1)(c), and 6(1)(f), including legitimate interests. It says Essential may use data for personalization, service operation, marketing communications, analytics, trend analysis, security, enforcement, feedback, reviews, and feature testing.

This is the factual privacy reason for Essential Patcher's telemetry blocking: the mod gives users local control over data leaving their own computer.

## Why This Mod Exists

Essential adds useful social and multiplayer features, but it also adds a commercial cosmetics economy, in-game purchase prompts, virtual currency, telemetry, ads, promotional UI, and remote account-based cosmetic control.

Essential Patcher exists because players should be able to:

- run Minecraft: Java Edition mods without unwanted telemetry;
- remove commercial prompts from their own local UI;
- avoid client-side revocation of local visual choices;
- use locally displayed cosmetics without interacting with payment flows;
- share cosmetic state with other consenting patcher users;
- keep control over software running on their own computer.

This project does not attack Essential's infrastructure, scrape its services, overload its systems, impersonate its users, or alter its account database. It is a local patcher.

## Minecraft EULA Basis

The Minecraft French EULA states that users who bought Minecraft: Java Edition are allowed to play and modify it by adding features, tools, and plugins, collectively called "Mods". It also states that a Mod is something developed by the user or a third party and not substantially composed of Mojang/Microsoft copyrighted code or content.

The EULA also says mods may generally be distributed, but not pirated or modified versions of the game client or server software, and not versions substantially composed of Mojang/Microsoft code or content.

Essential Patcher fits that modding model:

- it is a Java Edition mod;
- it is original patcher code;
- it is not a modified Minecraft client;
- it does not distribute Minecraft code;
- it does not distribute Minecraft assets;
- it does not claim official Mojang or Microsoft approval.

Minecraft's EULA is not a license to copy Essential's assets or code. It is relevant because it confirms that the Minecraft Java modding ecosystem is permitted when mods stay within the stated boundaries.

## EU Software Law Basis

Directive 2009/24/EC protects computer programs while also preserving specific user rights.

The project relies on these principles:

- Article 5(1): where no specific contractual provisions apply, acts necessary for use of a program by a lawful acquirer, including error correction, do not require authorization from the rightsholder.
- Article 5(3): a person with the right to use a copy of a program may observe, study, or test the functioning of the program to determine the ideas and principles underlying any element of the program while performing acts they are entitled to perform.
- Article 6: decompilation can be permitted when indispensable to obtain information necessary for interoperability of an independently created program with other programs, subject to strict limits.
- Article 8: contractual provisions contrary to Article 6 or to Article 5(2) and 5(3) are null and void.

Essential Patcher's position is that inspecting Essential's runtime behavior and class structure for the purpose of writing an independent patcher and interoperability layer falls within these software-law principles, especially in the EU.

This does not authorize copying or redistributing Essential's protected expression. That is why this repository must not contain Essential's decompiled source, modified binaries, models, textures, sounds, icons, or other assets.

## French Software Law Basis

For French users and maintainers, Article L122-6-1 of the Code de la propriete intellectuelle implements similar software exceptions.

The project relies on these principles:

- observation, study, and testing of software functioning may be allowed for a lawful user;
- reproduction or translation of code can be allowed when indispensable to obtain information necessary for interoperability of independently created software, subject to statutory conditions;
- information obtained for interoperability must not be used to create substantially similar infringing software or to harm the normal exploitation of the original software.

Essential Patcher is not a clone of Essential. It is not a competing social network or cosmetics store. It is a narrow runtime patcher and sync layer for users who already run Essential.

## Privacy Law Basis

GDPR does not say that users must accept all telemetry in software running on their own machine. It gives users rights and imposes duties on controllers.

Relevant GDPR provisions include:

- Article 5: personal data must be processed lawfully, fairly, transparently, for specified purposes, and limited to what is necessary.
- Article 6: processing requires a lawful basis.
- Article 7: consent, where relied on, must be demonstrable and withdrawable.
- Articles 12 to 14: data subjects must receive transparent information.
- Article 17: data subjects may have a right to erasure.
- Article 21: data subjects may object to processing based on public interest or legitimate interests, including profiling based on those grounds.
- Article 25: controllers must implement data protection by design and by default.
- Recital 38: children deserve specific protection with regard to personal data.

Essential's Privacy Policy states that it may collect Minecraft UUIDs, device and usage information, concurrent mods, IP address, unique identifiers, behavioral information, and data linked to email or Minecraft UUID. It also states that some processing relies on legitimate interests.

Essential Patcher's telemetry blocking is a user-side privacy control. It helps users reduce nonessential data disclosure from their own client. It does not break into Essential's systems or access anybody else's data.

## Consumer Protection Basis

Essential's Terms describe a commercial system built around Coins, Digital Goods, subscriptions, purchase prompts, Tebex checkout, limited licenses, revocable access, no transferability, and no ownership of Digital Goods.

The project does not ask a court to decide that every part of that system is unlawful. The narrower project position is that users may locally remove commercial pressure, sale prompts, ads, notices, and purchase UI from software running on their own machine.

EU consumer law supports careful scrutiny of aggressive, misleading, or manipulative commercial practices:

- Directive 2005/29/EC prohibits unfair business-to-consumer commercial practices, including misleading and aggressive practices that materially distort the economic behavior of the average consumer.
- Regulation (EU) 2022/2065, Article 25, prohibits certain online platform interface designs that deceive, manipulate, or materially distort users' ability to make free and informed decisions. The DSA does not automatically apply to every game mod UI, but it reflects a broader EU policy against manipulative interface design.
- French Consumer Code Article L212-1 restricts unfair terms that create a significant imbalance between professional sellers and consumers.

Essential Patcher's ad and prompt blocking is local user control. It does not interfere with paid transactions, does not automate fraud, and does not misrepresent a user's purchases to Essential.

## Terms of Service and Statutory Rights

Essential's Terms contain broad restrictions, including restrictions on reverse engineering and modification. Those clauses matter and may create contractual risk.

However, in the EU and France, statutory software rights can override contrary contract terms in specific circumstances. Directive 2009/24/EC Article 8 says terms contrary to Article 6 and Article 5(2) or 5(3) are null and void. French law has comparable software-interoperability protections.

The project position is therefore:

- Essential can set terms for its own services;
- users can still have mandatory statutory rights that cannot be contracted away;
- this repository should stay within those statutory rights by distributing original code only and avoiding copied assets, copied source, modified binaries, forged server calls, or commercial exploitation.

## Cosmetic Unlocking Boundary

Cosmetics are the highest-risk part of this project, so the boundary must stay clear.

Essential Patcher does not:

- buy, sell, transfer, refund, redeem, or exchange Essential Coins;
- create Coins;
- modify an Essential account balance;
- submit fake checkout completion;
- submit fake ownership records;
- alter Essential's server-side entitlement database;
- tell Essential's servers that the user purchased a cosmetic;
- distribute Essential cosmetic models or textures;
- make non-patcher users see patched cosmetics.

Essential Patcher does:

- change local client checks that decide what the local user may equip or render;
- save cosmetic selections locally;
- sync selected cosmetic IDs and settings through an independent HTTPS service;
- show synced cosmetics only to consenting patcher users.

That distinction matters. The project is local display and user-to-user sync, not payment fraud or account entitlement forgery.

## Telemetry and Ads Boundary

Essential Patcher may block telemetry, analytics, ads, promotional notices, purchase screens, checkout packets, coin purchase flows, and similar commercial or tracking behavior.

The project should keep this boundary:

- block local client telemetry and commercial UI;
- do not attack Essential endpoints;
- do not overload Essential services;
- do not scrape private data;
- do not bypass authentication to access someone else's account;
- do not submit false reports, false purchases, or false account data.

Blocking data from leaving a user's own computer is materially different from unauthorized access to someone else's computer or server.

## Independent HTTPS Sync Service

Essential Patcher's HTTPS sync server is independent from Essential. It exists because patcher users cannot rely on Essential's official cosmetic sync once client-side checks and entitlement logic are changed.

The sync service should follow these privacy rules:

- store only what is needed for cosmetic sync;
- avoid storing IP addresses in the application database unless needed for abuse prevention;
- do not sell sync data;
- do not use sync data for ads;
- do not share sync data with Essential;
- use short-lived authentication/session data where possible;
- document what data is transmitted;
- provide deletion or reset mechanisms if the service is public.

This keeps the project consistent with its own privacy rationale.

## What This Repository Must Not Include

To preserve the legal position, the repository and releases must not include:

- Essential's original JAR;
- a modified Essential JAR;
- decompiled Essential source files;
- Essential assets, cosmetics, textures, sounds, models, icons, or branding;
- Minecraft's client or server JAR;
- a modified Minecraft client or server;
- decompiled Minecraft source files;
- Minecraft assets except ordinary mod metadata allowed by Minecraft's rules;
- payment bypass instructions;
- secrets, JWT keys, server credentials, tokens, or private VPS data;
- claims that the mod is official or endorsed;
- claims that users officially own unpaid Essential cosmetics.

## What Essential Patcher Does Not Claim

Essential Patcher does not claim:

- that Essential is required to support patched clients;
- that Essential must provide server-side entitlements;
- that Essential's Terms are invalid everywhere;
- that every court would agree with this document;
- that using the mod has zero account or contractual risk;
- that this document prevents takedowns or legal threats;
- that users own unpaid Essential Digital Goods.

## Technical Evidence Appendix

This project was written after inspecting Essential's runtime behavior and class structure for interoperability and compatibility.

Relevant observed areas include:

- cosmetic entitlement and wardrobe checks;
- equipped outfit storage and update flow;
- cosmetic revocation behavior;
- in-game cosmetic and emote sync behavior;
- telemetry managers and analytics calls;
- promotional notices and sale UI;
- checkout and coin purchase packet paths.

Those observations justify why the patcher targets local cosmetic checks, telemetry, ads, notices, checkout flows, and outfit sync. The repository should describe behavior at a high level and should not publish copied Essential implementation code.

## Final Position

Essential Patcher is an unofficial, free, open-source, client-side Minecraft: Java Edition mod. It distributes original code, uses runtime patching, avoids third-party assets and binaries, does not forge purchases, does not alter Essential's server-side account records, and gives users control over privacy, ads, and local cosmetic display.

Its legal basis is strongest when the project remains narrow:

- original code only;
- local client runtime patching only;
- no redistributed Essential or Minecraft assets;
- no modified third-party JARs;
- no fake purchases or fake entitlements;
- no commercial exploitation;
- transparent privacy practices for its own sync service;
- clear unofficial status.

That is the basis on which this project is maintained.
