# The banking example, with meaning

`examples/banking` is unchanged. `registry/` here adds atlas facts on the
same ids its model uses, and meta-source reads both and merges them, one
entity per id: what Overarch says an element is and draws, and what it is
about, reads, returns and depends on.

- `architecture.edn`: the API application's controllers and components.
  Each has a type and aspects (`#{:domain/auth :tier/api
  :capability/sign-in}`); an endpoint says what it reads and returns
  (`:auth/username :auth/password` → `:auth/token`); each part says what it
  depends on.
- `domain.edn`: a small DDD ontology (aggregate, command, domain event,
  policy: what each must say, and type-refs for what its relations mean),
  and the model's domain elements connected by it: which command acts on
  the account aggregate and emits which event, what the overdraft policy
  reacts to, which system keeps the accounts.
- `use-cases.edn`: which endpoint serves each use case, and which command
  it issues.
- `invariants.edn`: what should hold across the three. Each is checked over
  the merged entities, so a rule can use atlas's facts and Overarch's
  together ("an aggregate is kept by a `:system` or `:container`").

## Run it

With [meta-source](https://github.com/semantic-namespace/meta-source)
(babashka; nothing is loaded), from the repository's root, where
`meta-source.edn` is:

```sh
meta-source check --brief
meta-source where :banking.internet-banking.api-application/sign-in-controller
meta-source data :auth/token
```

The check reports three gaps in the example:

- nothing reacts to `:banking.domain/account-created-event`;
- `accounts-summary-controller` reads the auth token and never reaches the
  security component;
- the "transfer money" use case is served by no API endpoint: the model has
  none for transfers.

and, as info, the relations these facts state that the model doesn't draw
(its domain elements have none).

## atlas's registry, from both

`bb atlas-store`, in this directory, writes what meta-source merges as
atlas's registry into a git store (`../../.atlas-store`, one commit per
change). atlas's own questions then work across the model and the facts,
with babashka:

```sh
bb --config <atlas>/bb.edn registry blast-radius ../../.atlas-store HEAD :banking.mainframe/mainframe-banking-system
bb --config <atlas>/bb.edn registry consumers-of ../../.atlas-store HEAD :auth/token
bb --config <atlas>/bb.edn registry by-aspect ../../.atlas-store HEAD :domain/auth
```

Changing the mainframe system reaches the facade and the accounts summary
(the architecture), the account aggregate and both commands (the domain),
and both use cases.

The same questions as an MCP server over stdio, for an LLM, also with
babashka (`by_aspect`, `entity_detail`, `producers_of`, `consumers_of`,
`dependencies`, `dependents`, `blast_radius`, `trace_causes`, `overview`):

```sh
bb --config <atlas>/bb.edn registry-mcp ../../.atlas-store
```
