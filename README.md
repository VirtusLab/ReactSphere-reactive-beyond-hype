# ReactSphere-reactive-beyond-hype
Repo for presentation on ReactSphere: Reactive beyond hype

### Requirements:
 * sbt
 * Docker

### Testing:
```
sbt test
```

### Running it:
```bash
sbt docker:publishLocal
```
and then...
```bash
docker run --rm -it -p 8080:8080 hello-world-async:0.1.0-SNAPSHOT
```
or:
```bash
docker run --rm -it -p 8080:8080 hello-world-sync:0.1.0-SNAPSHOT
```

and then:
```bash
curl -i localhost:8080
```