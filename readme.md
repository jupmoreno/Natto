# Natto

> This project was made for our Communication Protocols subject at ITBA University (Buenos Aires, Argentina).

# Usage

    [<server_address> [<server_port>]] [--psp-port <port>] [--xmpp-port <port>]
    Where:
     <server_address>   : Sets the XMPP server address
     <server_port>      : Sets the XMPP server port number (default: 5222)
     --psp-port <port>  : Sets the proxy's PSP listening port number (default: 1081)
     --xmpp-port <port> : Sets the proxy's XMPP listening port number (default: 1080)

# Development

Run the following commands from the project's root directory. 

If gradle isn't installed in your system, replace `gradle` command below with `./gradlew`.

## Command Line

### Build

    gradle build
    
### Run

    gradle run

## Intellij's IDEA

### Setup

Select `Import project`, then navigate to the project's root directory. Then check `Import project from external model` and select `Gradle`.

Or from the command line run:

    gradle idea
    
Then open the recently created `.ipr` file with Intellij's IDEA.
    
### Clean

    gradle cleanIdea
    gradle cleanWorkspace
