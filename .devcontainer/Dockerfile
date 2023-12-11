FROM ubuntu:22.04

ARG USERNAME=devuser
ARG USER_UID=1234
ARG USER_GID=$USER_UID

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m -s /bin/bash $USERNAME \
    #
    # [Optional] Add sudo support. Omit if you don't need to install software after connecting.
    && apt-get update \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

RUN apt-get update && apt-get -y install build-essential \
    g++-multilib \
    libxt-dev libxtst-dev\
    vim git gawk

COPY j2sdk-1_4_2_19-linux-i586.bin /tmp

RUN mkdir -p /opt \
    && cd /opt \
    && echo yes | /tmp/j2sdk-1_4_2_19-linux-i586.bin

ENV JDK_DIR=/opt/j2sdk1.4.2_19

ENV LC_ALL=C.UTF-8

USER ${USERNAME}
