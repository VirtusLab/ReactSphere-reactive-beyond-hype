# ReactSphere-reactive-beyond-hype
Repo for presentation on ReactSphere: Reactive beyond hype

### Requirements:
 * ammonite
 * sbt 
 * Docker
 * kubectl

### Development setup:

##### Adjust VM memory sizes in Vagrantfile:

Find these lines in `tectonic/Vagrantfile` and adjust them to your needs:
```ruby
$controller_vm_memory = 2048
$worker_vm_memory = 4096
```

##### Modify /etc/hosts for local cluster ingresses (entry points)

1. Open `/etc/hosts` with superuser permissions
2. Add line `172.17.4.201    docker-registry.local`
3. Add line `172.17.4.201    hello-world-async.local`
4. Add line `172.17.4.201    hello-world-sync.local` 
4. Save it

##### Starting Tectonic Kubernetes local cluster

1. Start Vagrant machines and wait until you get the `Tectonic has started successfully!` message:
```bash
cd tectonic-1.7.5
vagrant up
```
2. Log into Tectonic Console @ `https://console.tectonicsandbox.com` using username `admin@example.com` and password `sandbox`
3. Go to `https://console.tectonicsandbox.com/settings/profile`, click `Download Configuration` button, confirm sign in and then
   download configuration file for kubectl, then move it to 

### Testing microservices:
```
cd codebase
sbt test
```

### Running it:

Run everything in `codebase` directory in sbt shell after project change (ie.: `project hello-world-sync`):

```
docker:publishLocal
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

### Deploying Docker Registry to Tectonic Cluster

Run ammonite script to set up docker registry in Tectonic cluster:

```bash
amm infra/scripts/setup-docker-registry.sc
```


### Publishing to in-cluster Docker Registry:

Tag images published locally to be able to push them to cluster registry:

```bash
docker tag hello-world-async:0.1.0-SNAPSHOT docker-registry.local/hello-world-async
```
or
```bash
docker tag hello-world-sync:0.1.0-SNAPSHOT docker-registry.local/hello-world-sync
```

and then push them:
```bash
docker push docker-registry.local/hello-world-async
```
or
```bash
docker push docker-registry.local/hello-world-async
```

### Running applications in cluster:

If publishing to cluster registry went well you can deploy apps to k8s using this command:
```bash
kubectl apply -f infra/manifests/hello-world-sync.dev.yaml
```
or
```bash
kubectl apply -f infra/manifests/hello-world-async.dev.yaml
```

After some time (you can track deployment progress in Tectonic Console - Pods in namespace `microservices`) 
you will be able to execute following curls:
```bash
curl -ik https://hello-world-async.local
```
or
```bash
curl -ik https://hello-world-sync.local
```