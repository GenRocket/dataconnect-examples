# REST CI fixture

Use a dedicated server user and a protected, explicitly enabled CI job because
generation consumes quota.

```bash
java -jar examples/target/dataconnect-examples.jar rest fixture --loop-count 3 --output output/ci-fixture.json
```
