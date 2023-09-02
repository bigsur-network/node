FROM haskell:8 as bnfc-build
LABEL Temporary container for building BNFC
RUN cabal update
RUN cabal install happy alex
WORKDIR /var/tmp/bnfc
ARG BNFC_COMMIT=ce7fe1fd08d9d808c14ff626c321218c5b73e38b
RUN git clone https://github.com/BNFC/bnfc . && git checkout $BNFC_COMMIT
RUN stack init && stack setup
RUN stack build && stack install --local-bin-path=.

FROM ubuntu:20.04
LABEL Build environment for https://github.com/Rhonix-Network/RhonixNode for Debian 20.04
COPY --from=bnfc-build /var/tmp/bnfc/bnfc /usr/local/bin/
RUN apt-get update
# java
RUN apt-get install -y --no-install-recommends openjdk-17-jre
# sbt
# https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html#Ubuntu+and+other+Debian-based+distributions
RUN	apt-get update
RUN	apt-get install -y --no-install-recommends apt-transport-https curl gnupg -yqq
RUN	echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN	echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN	curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN	chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN	apt-get update
RUN	apt-get install -y --no-install-recommends sbt
# jflex
RUN apt-get install -y --no-install-recommends jflex
# git
RUN apt-get install -y --no-install-recommends git
# python venv any yaml
RUN apt-get install -y python3-venv
RUN apt-get install -y pip
RUN pip install pyyaml