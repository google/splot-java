# Security Traits

The `security` Splot trait collection defines traits for implementing the
[Splot Security Model](../../ssm/intro.md).

 * [`Keychain`](./keychain.md): Trait for things that manage [Keychain Items](../../ssm/keychain.md#keychain-items).
 * [`KeychainItem`](./keychain-item.md): Trait for things that represent cryptographic credentials.
 * [`Identity`](./identity.md): Trait for things that represent [authenticated client identities](../../ssm/identities.md).
 * [`AccessRule`](./access-rule.md): A trait for a single
   [access rule](../../ssm/identities.md#access-rules) that is associated with a
   specific [identity](../../ssm/identities.md).
