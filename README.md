# Welcome to the Cotton project!

## What Cotton is about?
The purpose of this project is to create a **scalable cloud system** capable of **storing** and **processing** data in the cloud. The main focus of this project is **scalability**, the cloud needs to be prepared for an extreme increase or decrease of usage and then scale according to the changing demands. Beside the scaling, the system is required to be **fault tolerant** and **secure**.

## Current approach
The current approach consists of five components:
* Service Discovery
  * Local Service Discovery
  * Global Service Discovery
* Service Handler
* Network Handler
* Internal Routing
* Request Queue

## To use the Cloud System
In order to use the cloud system the developer needs to make:
* Service implementing the Service interface and then register it to the cloud.
* Client instantiating an cotton instance.
* The current approach running.