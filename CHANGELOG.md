# Changelog

## 1.0.7

- Cosmetic sync no longer calls Mojang at all. This stops the "high API requests" temp-suspensions that were still happening under the previous access-token flow.
- Fixed: the 1.20.1 mixin config referenced a `ParticleEngineMixin` that doesn't exist.