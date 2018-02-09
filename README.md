# ReactSphere-reactive-beyond-hype
Repo for presentation on ReactSphere: Reactive beyond hype

### Requirements:
 * ammonite
 * sbt
 * Docker

### Development setup:

##### Modifying /etc/hosts for local Docker Registry domain

1. Open `/etc/hosts` with superuser permissions
2. Add line `172.17.4.201    docker-registry.local`, this will point local domain to tectonic VM ip
3. Save it

##### Adjust VM memory sizes in Vagrantfile:

Find these lines in `tectonic/Vagrantfile` and adjust them to you needs:
```ruby
$controller_vm_memory = 2048
$worker_vm_memory = 4096
```

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
