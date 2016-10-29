# Natto

> This project was made for our Communication Protocols subject at ITBA University (Buenos Aires, Argentina).

# Usage

    $> natto [<address>] [<port>] [--config-file (-c) <path>] [--psp-port <port>] [--xmpp-port <port>]
    Where:
     <address>                 : Sets the default XMPP server hostname
     <port>                    : Sets the default XMPP server port
     --config-file (-c) <path> : Sets the proxy's config file (default: config.xml)
     --psp-port <port>         : Sets the proxy's PSP listening port number
     --xmpp-port <port>        : Sets the proxy's XMPP listening port number

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
