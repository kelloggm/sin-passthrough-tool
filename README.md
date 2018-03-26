# Passthrough

This tool help you to present a service on the Internet. It will create
an EC2 instance (t2.micro) to handle incoming traffic and redirect it
to your computer or server with a SSH tunnel forwarding.

## How to use it

1. First, you must have an AWS account and create AWS Access Key and Secret
   Key. You can find a guide here : https://docs.aws.amazon.com/general/latest/gr/managing-aws-access-keys.html

1. Download a JAR in the release section of GitHub.

1. Setup your environment with:
    - `export AWS_REGION=<YOUR NEAREST AWS REGION>`
    - `export AWS_ACCESS_KEY_ID=<YOUR AWS CREDENTIALS>`
    - `export AWS_SECRET_ACCESS_KEY=<YOUR AWS CREDENTIALS>`
    - `export CLOUD_PROVIDER=AWS`
    
1. Run the passthrough with:

    `java -jar passthrough.jar <remote port>:<destination host>:<destination port> <remote port>:<destination host>:<destination port> ...`
   
   Example of couple ports:
     - `80:80`
     - `443:192.168.1.254:443`
     - `8080:80`
    
   You can specify couple of ports for the same command
