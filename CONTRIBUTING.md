# Contributing

## Adding a new function

### Java Driver

- Add the function in `query.Language`
- Add any overloaded variants if there are cases in which params can be Java primitives.
  - E.g: `ContainsStr(Expr value, Expr search)` and `ContainsStr(String value, Expr search)`
- Add serialization test case in `SerializationSpec`
- Add integration test case in `faunadb.client.ClientSpec`

### Scala Driver

- Add function in `query.package`
  - No overloads are needed in this case, since we provide some implicit conversions that make those on the fly
- Add integration test case in `ClientSpec`
