#!/bin/sh
SERVER_NAME="${project.artifactId}"
JAR_NAME="${project.artifactId}-${project.version}.jar"

PROP_LIST="${project.properties}"
APP_NAME=$(echo "$PROP_LIST" | grep -o 'mainClazz.*,' | awk -F "," '{print $1}' | awk -F "=" '{print $2}')

USER=`whoami`

cd `dirname $0`   # 进入bin目录
BIN_DIR=`pwd`     # bin目录绝对路径
cd ..             # 返回到上一级项目根目录路径
PROJECT_DIR=`pwd`  # `pwd` 执行系统命令并获得结果

IS_DOCKER_ENV=false
if [ -f "/.dockerenv" ]; then
    IS_DOCKER_ENV=true
fi

# Java命令，取操作系统环境变量: JAVA_HOME
JAVA_CMD=java
if [ "$JAVA_HOME" != "" ]; then
    JAVA_CMD=$JAVA_HOME/bin/java
fi

# 内存配置，默认: 1g
JAVA_MEM_OPTS="-server -Xms2g -Xmx2g"
if [ "$JAVA_OPTS" != "" ]; then
    JAVA_MEM_OPTS="-server $JAVA_OPTS"
fi

JAR_PATH="$PROJECT_DIR/lib/$JAR_NAME"
CLASSPATH=$PROJECT_DIR:$PROJECT_DIR/lib/*.jar

CP_PARAM=.:$PROJECT_DIR/lib/*

CONFIG_FILES="-Dspring.config.location=$PROJECT_DIR/config/ "

help() {
  echo "Usage: app.sh [start|start4Log|stop|restart|status|dump]"
  exit 1
}

is_exist(){
  pid=""
  if [ "$IS_DOCKER_ENV" = "true" ]; then
    pid=$(ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $1}')
  else
    pid=$(ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $2}')
  fi
  if [ -z "$pid" ]; then
    return 0 # 不存在
  else
    return 1 # 存在
  fi
}

start(){
  is_exist
  if [ $? -eq "1" ]; then
    echo "$SERVER_NAME is already running, PID=${pid}"
  else
    if [ "$IS_DOCKER_ENV" = "true" ]; then
      $JAVA_CMD $CONFIG_FILES $JAVA_MEM_OPTS -Duser.timezone=GMT+8 -Duser.language=en -cp $CP_PARAM $APP_NAME > /dev/stdout 2>&1
    else
     nohup $JAVA_CMD $CONFIG_FILES $JAVA_MEM_OPTS -Duser.timezone=GMT+8 -Duser.language=en -cp $CP_PARAM $APP_NAME  > /dev/null 2>&1 &
    fi
    echo "$SERVER_NAME is started!"
  fi
}

start4LocalLog(){
  is_exist
  if [ $? -eq "1" ]; then
    echo "$SERVER_NAME is already running, PID=${pid}"
  else
    if [ "$IS_DOCKER_ENV" = "true" ]; then
      $JAVA_CMD $CONFIG_FILES $JAVA_MEM_OPTS -Duser.timezone=GMT+8 -Duser.language=en -cp $CP_PARAM $APP_NAME > /dev/stdout 2>&1
    else
     nohup $JAVA_CMD $CONFIG_FILES $JAVA_MEM_OPTS -Duser.timezone=GMT+8 -Duser.language=en -cp $CP_PARAM $APP_NAME  > start.log 2>&1 &
    fi
    echo "$SERVER_NAME is started!"
  fi
}

stop(){
  is_exist
  if [ $? -eq "1" ]; then
    if [ "$IS_DOCKER_ENV" = "true" ]; then
      stop4docker
    else
      stop4graceful
    fi
  else
    echo "$SERVER_NAME is not running"
  fi
}

stop4graceful(){
  for pid in `ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $2}'`
  do
    echo "killing $pid ......"
    kill $pid
  done

  sleep 3	 ##sleep a while,then check pid is killed

  for pid in `ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $2}'`
  do
    echo "killing -9 $pid ......"
    kill -9 $pid
  done
}

stop4docker(){
  for pid in `ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $1}'`
  do
    echo "killing $pid ......"
    kill $pid
  done

  sleep 3	 ##sleep a while,then check pid is killed

  for pid in `ps -ef|grep $USER|grep $SERVER_NAME|grep -v grep |grep -v '{app.sh}' |awk '{print $1}'`
  do
    echo "killing -9 $pid ......"
    kill -9 $pid
  done
}


restart(){
  stop
  sleep 3
  start
}

status(){
  is_exist
  if [ $? -eq "1" ]; then
    echo "$SERVER_NAME is running, PID=${pid}"
  else
    echo "$SERVER_NAME is not running"
  fi
}

dump(){
  is_exist
    if [ $? -eq "1" ]; then
      DUMP_DIR="$PROJECT_DIR/dump"
      if [ ! -d $DUMP_DIR ]; then
          mkdir $DUMP_DIR
      fi
      do_dump
    else
      echo "$SERVER_NAME is not running"
    fi
}

do_dump(){
    jstack -l $pid > $DUMP_DIR/jstack-$pid.dump 2>&1
    echo -e ".\c"
    jstat -gcutil $pid > $DUMP_DIR/jstat-gcutil-$pid.dump 2>&1
    echo -e ".\c"
    jmap $pid > $DUMP_DIR/jmap-$pid.dump 2>&1
    echo -e ".\c"
    jmap -histo $pid > $DUMP_DIR/jmap-histo-$pid.dump 2>&1
    echo -e ".\c"
    jmap -heap $pid > $DUMP_DIR/jmap-heap-$pid.dump 2>&1
    echo -e ".\c"
    if [ -r /usr/bin/free ]; then
      /usr/bin/free -m > $DUMP_DIR/free.dump 2>&1
      echo -e ".\c"
    fi
    echo "OK!"
    echo "DUMP: $DUMP_DIR"
}

case "$1" in
  "start")
    start
    ;;
  "start4Log")
    start4LocalLog
    ;;
  "stop")
    stop
    ;;
  "status")
    status
    ;;
  "restart")
    restart
    ;;
  "dump")
      dump
      ;;
  *)
    help
    ;;
esac
exit 0
