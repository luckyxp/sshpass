### 跨平台的sshpass(linux,windows,mac可用)
##### ssh -c
`java -jar sshpass.jar -h %remote_host% -u %remote_host_user% -p %remote_host_passwd% -c "ls -l"`
##### scp
`java -jar sshpass.jar -h %remote_host% -u %remote_host_user% -p %remote_host_passwd% --scp local_file remote_file`
