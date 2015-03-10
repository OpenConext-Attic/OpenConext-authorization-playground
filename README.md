# OpenConext-authorization-playground
OAuth2 demo client for the Oauth2 server for the OpenConext platform.

## Development

To run locally:

`mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"`

Or use the shortcut:

    ./start.sh

### Create an alias for localhost in your /etc/hosts
To avoid overwriting cookies add the following line to /etc/hosts:

    127.0.0.1 	authz-playground-local


That's it. Point your browser to [http://authz-playground-local:8089](http://authz-playground-local:8089)

## Editing CSS

We use sass to ease the pain of CSS development. We recommend you install sass using ruby. Best is to manage your rubies
with [rbenv](https://github.com/sstephenson/rbenv). After installing rbenv ```cd``` into this directory and run:

    gem install sass

Then run

    sass --watch src/main/sass/application.sass:src/main/resources/static/css/application.css

Or use the shortcut:

    ./watch.sh
