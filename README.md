[![Build Status](https://travis-ci.org/VirtusLab/ReactSphere-reactive-beyond-hype.svg)](https://travis-ci.org/VirtusLab/ReactSphere-reactive-beyond-hype)
[![Coverage Status](https://coveralls.io/repos/github/VirtusLab/ReactSphere-reactive-beyond-hype/badge.svg)](https://coveralls.io/github/VirtusLab/ReactSphere-reactive-beyond-hype)

 # Reactive: Beyond Hype

       by Lukasz Bialy, Marcin Zagorski, Pawel Dolega @ VirtusLab 2018
       Repo for presentation on ReactSphere: Reactive beyond hype

### Requirements (with install instructions:)
 * ammonite ( tested on ver. 1.0.x, http://ammonite.io/#Ammonite-REPL )
 * Docker ( tested on ver. 17.12.x, https://www.docker.com/get-docker , also make sure you start service e.g. `sudo systemctl start docker` and add your user to docker group `sudo usermod -a -G docker $YOUR_USER`)
 * kubectl (https://kubernetes.io/docs/tasks/tools/install-kubectl/ )
 * Vagrant ( tested on ver. 2.0.x, https://www.vagrantup.com/downloads.html )

### Prerequistes

Make sure to export following environment variables on your host:

* Key and secret for user: `billing-service-worker`

```bash
export BILLING_WORKER_AWS_KEY=xxxxxxxxxxxx
export BILLING_WORKER_AWS_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
 
### Development setup:

First of all you may run the whole stack locally via on docker containers or run whole tectonic k8s cluster. The latter is much more aligned with the real production configuration.

#### Container local setup

Local setup with dockerized containers is extremely easy.

For sync stack setup you run (you need to be in repo root directory):

`amm infra/scripts/build-run-docker.sc --stack sync`

For async you do:

`amm infra/scripts/build-run-docker.sc --stack async`

You have several options to run this script (e.g. with without test execution or container image re-publish) - just use ``amm infra/scripts/build-run-docker-sync.sc` 
(without arguments) and it will print all the information you may need.

This script will generally build code, wrap containers, run Cassandra and all services (sync or async ones) locally.

#### Kubernetes local setup

##### Adjust VM memory sizes in Vagrantfile:

Find these lines in `tectonic/Vagrantfile` and adjust them to your needs:
```ruby
$controller_vm_memory = 2048
$worker_vm_memory = 4096
```

##### Modify /etc/hosts for local cluster ingresses (entry points)

* Open `/etc/hosts` with superuser permissions
* Add line `172.17.4.201    docker-registry.local`
* Add line `172.17.4.201    auction-house-primary.local` 
* Add line `172.17.4.201    billing-service-secondary.local
* Add line `172.17.4.201    identity-service-tertiary.local
* Add line `172.17.4.201    payment-system.local

* Save it

##### Starting Tectonic Kubernetes local cluster

1. Start Vagrant machines and wait until you get the `Tectonic has started successfully!` message:
```bash
cd tectonic-1.7.5
vagrant up
```
2. Log into Tectonic Console @ `https://console.tectonicsandbox.com` using username `admin@example.com` and password `sandbox`
3. Go to `https://console.tectonicsandbox.com/settings/profile`, click `Download Configuration` button, confirm sign in and then
   download configuration file for kubectl, then move it to indicated location.

#### Tectonic local setup

Tectonic local setup is very similar to bare Docker setup. Just use the script:

`amm infra/scripts/build-run-kubernetes.sc --stack sync`

Arguments & flags are exactly the same as the ones for docker setup (so you can switch between sync / async etc).

### Deploying cluster to AWS

##### Initial setup

You will need:

1. Tectonic licensing files:
    * license.txt
    * pull_secret.json
    
    Those can be obtained from coreos.com - free licence allows for clusters
    with up to 10 nodes.
    
    You have to put those files into `./cluster` or `./cluster-mini` directory
    depending on which cluster configuration you want to use.
    
2. AWS API key and secret:
    You can create those for your account in AWS Console. When you have them
    set those environment variables:
    
    ```
    AWS_ACCESS_KEY_ID // self-explanatory
    AWS_SECRET_ACCESS_KEY // self-explanatory
    ```
      
3. Terraform tool installed

4. Quay.io docker registry sign in token

##### Cluster deployment configuration

There are two AWS Tectonic cluster configuration options available:

* cluster-mini: 4x t2.medium based development cluster consisting of:
  - 1x t2.medium for etcd
  - 1x t2.medium for master node
  - 2x t2.medium for worker nodes
  
* cluster: large deployment designed to handle large traffic and full 
  multi-pod replica sets
  - 1x t2.medium for etcd
  - 1x m4.large for master node
  - 4x m4.large for worker nodes

1. You have to set additional environment variables to configure your deployment:

```
CLUSTER // set to desired cluster name: reactsphere-mini or reactsphere
TF_VAR_tectonic_admin_email // set to desired admin account login
TF_VAR_tectonic_admin_password // set to desired admin password
```

2. Execute `docker login` to sign docker daemon into Quay.io registry.

Gotchas:

* Region configuration: if you want to override AWS region via `AWS_REGION`
  environment variable remember to modify subnet configuration in 
  `terraform.tfvars` file (it's the `tectonic_aws_master_custom_subnets`
  and `tectonic_aws_worker_custom_subnets` vars, modify region and AZ only,
  this configuration is required for single-AZ deployment and it's HIGHLY
  RECOMMENDED to deploy to single AZ only due to incidental latencies problem
  in cross-AZ clusters).
  
##### Cluster deployment

Depending on cluster configuration you want to use go to either `./cluster` or
`./cluster-mini` and execute `./boot.sh` script. If your config is correct
terraform will initialize itself, validate deployment plan against AWS and then
proceed to deployment. You will be asked to confirm deployment - there should be 
136 elements to deploy for `cluster-mini` and few more for full `cluster` - this 
number might be smaller if you are retrying failed deployment - terraform is able
to continue broken deployment by performing delta of plan and existing state in
AWS. When asked to confirm enter 'yes' and press enter. Deployment takes about 10
minutes but then docker has to pull all the images and that elongates the process
to about ~30-40 minutes. 

After terraform is done you can use generated kubeconfig to access cluster via 
`kubectl` tool. To do this you have to set environmental variable KUBECONFIG to
path pointing to `(pwd)/cluster-mini/generated/auth/kubeconfig` or 
`(pwd)/cluster/generated/auth/kubeconfig` respectively. Note the pwd in front of path
- value of KUBECONFIG is meant to be full, non-relative path to `kubeconfig` file
and therefore pwd means full path to the root of this project.

You can check if ApiServer is up with `kubectl get nodes` and also if Tectonic 
Console responds at `https://reactsphere-mini.beyondthehype.pl` 
or `https://reactsphere.beyondthehype.pl` respectively to cluster size chosen. 
Don't  worry about DNS resolution errors - propagation takes a bit of time so if 
kubectl lists nodes correctly it's just a matter of time. When all nodes are in 
Ready state you can execute `kubectl get pods --all-namespaces` to monitor system 
and tectonic pod deployments.

---
 
### Testing microservices:
```
cd codebase
./sbt test
```

### Deploying Docker Registry to Tectonic Cluster

Run ammonite script to set up docker registry in Tectonic cluster:

```bash
amm infra/scripts/kubernetes/setup-docker-registry.sc
```

Docker Registry uses SSL with a self-signed certificate (present in `infra/certs`). While this script
takes care of configuring Docker daemons running in Tectonic's VMs you still have to configure daemon
on your local host machine. You have to copy the file `tectonic-1.7.5/provisioning/docker/daemon.json`
to directory containing Docker daemon configuration on your system. Look [here](https://docs.docker.com/registry/insecure/)
for path to modify (or UI segment to enter configuration if on Mac - word of warning here - if form for
unsecure registries crashes daemon on startup in Docker for Mac you have to use `advanced` and manually
modify the json  ¯\_(ツ)_/¯).


### Publishing to in-cluster Docker Registry (dev only):

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
docker push docker-registry.local/hello-world-sync
```

If you have in-cluster docker registry deployed you can just run this is `./sbt` console:
```
project helloWorldAsync
docker:publish
```

This will build docker image and publish it directly to cluster.

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

### In-cluster Cassandra

Deploying Cassandra cluster:
```bash
kubectl apply -f infra/manifests/cassandra.dev.yaml
```

Applying schema migrations:
- First wait for both Cassandra instances to start and settle - check out pod logs to see if they are ready for work

Run migration job:
```bash
kubectl apply -f infra/manifests/migration.dev.yaml
```

Verify migration success:
 - check job successful runs in Tectonic Console, namespace: databases, tab jobs.
 - connnect to Cassandra cluster using cqlsh tool:
```bash
kubectl -n databases run --rm -i --tty cqlsh --image=cassandra --restart=Never -- sh -c 'exec cqlsh cassandra-0.cassandra.databases.svc.cluster.local'

cqlsh> DESCRIBE KEYSPACE microservices;
```

### Deploying Auction House Primary microservices to Tectonic Cluster

Start by publishing images to in-cluster docker registry using `./sbt` console:
 
```
project auctionHousePrimarySync
docker:publish
project auctionHousePrimaryAsync
docker:publish
```

Then apply kubernetes manifests:
```bash
kubectl apply -f infra/manifests/auction-house-primary-sync.dev.yaml
```
and
```bash
kubectl apply -f infra/manifests/auction-house-primary-async.dev.yaml
```

After a while you will be able to call both services (remember to edit `/etc/hosts` to enable DNS mappings):

```bash
curl -ik https://auction-house-primary-sync.local/_status
```
or
```bash
curl -ik https://auction-house-primary-async.local/_status
```

### Running gatling tests

To run tests make sure that server is started and correct address is pointed in config. Then exec:
```bash
./sbt gatling:test
```
