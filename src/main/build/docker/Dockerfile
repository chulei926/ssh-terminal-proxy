FROM openjdk:8-alpine
MAINTAINER chulei
ADD @project.artifactId@-@project.version@.tar.gz /
RUN cd / && \
    mv @project.artifactId@-@project.version@ @project.artifactId@ &&  \
    echo "#!/bin/sh" >> /bin/startApp &&  \
    echo "/@project.artifactId@/bin/app.sh start && sh" >> /bin/startApp &&  \
    chmod +x /bin/startApp
WORKDIR /@project.artifactId@
VOLUME /mnt/logs
CMD ["startApp"]
