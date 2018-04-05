#!/usr/bin/env bash

command -v terraform >/dev/null 2>&1 || { echo >&2 "You have to install terraform first.  Aborting."; exit 1; }

export AWS_REGION=eu-west-1

: "${AWS_ACCESS_KEY_ID?Please set environment variable AWS_ACCESS_KEY_ID}"
: "${AWS_SECRET_ACCESS_KEY?Please set environment variable AWS_SECRET_ACCESS_KEY}"
: "${TF_VAR_tectonic_admin_email?Please set environment variable TF_VAR_tectonic_admin_email}"
: "${TF_VAR_tectonic_admin_password?Please set environment variable TF_VAR_tectonic_admin_password}"

if [ ! -f pull_secret.json ]; then
    echo "Pull secret file missing!"
    exit 1
fi

if [ ! -f license.txt ]; then
    echo "License file missing!"
    exit 1
fi

terraform init -var-file=terraform.tfvars platforms/aws

terraform plan -var-file=terraform.tfvars platforms/aws

terraform apply -var-file=terraform.tfvars platforms/aws