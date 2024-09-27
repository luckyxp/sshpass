import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

public class SshPass {
    public static class Args {
        @Parameter(names = "-h", description = "remote host", required = true)
        private String host;
        @Parameter(names = "-u", description = "username", required = true)
        private String user;
        @Parameter(names = "-p", description = "password", required = true)
        private String password;

        @Parameter(names = "-c", description = "command")
        private String command;

        @Parameter(names = "--scp",description = "scp")
        private boolean scp;

        @Parameter(names = "--help", help = true)
        private boolean help;

        private ScpArgs scpArgs;
    }

    public static class ScpArgs {
        @Parameter(names = "-i", description = "scp input file", required = true)
        private String input;
        @Parameter(names = "-o", description = "scp output file", required = true)
        private String output;
    }

    public static void main(String[] arg) {
        Args args = new Args();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(args)
                .acceptUnknownOptions(true)
                .build();
        try {
            jCommander.parse(arg);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            return;
        }
        if (args.scp) {
            ScpArgs scpArgs = new ScpArgs();
            JCommander scpCommander = JCommander.newBuilder()
                    .addObject(scpArgs)
                    .acceptUnknownOptions(true)
                    .build();
            try {
                scpCommander.parse(arg);
            } catch (Throwable e) {
                System.err.println(e.getMessage());
                return;
            }
            if (args.help) {
                scpCommander.usage();
                return;
            }
            args.scpArgs = scpArgs;
        } else if (args.command == null) {
            System.err.println("The following option is required: [-c], [--scp]");
            return;
        }

        if (args.help) {
            jCommander.usage();
            return;
        }
        doMain(args);
    }

    public static void doMain(Args args) {
        try {
            // 创建一个 SSHSession
            JSch jsch = new JSch();
            Session session = jsch.getSession(args.user, args.host, 22);
            session.setPassword(args.password);

            // 关闭严格主机密钥检查（根据需要调整）
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            if (!args.scp) {
                // 执行 SSH 命令
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(args.command);
                channelExec.setErrStream(System.err);
                channelExec.setInputStream(System.in);

                // 输出结果
                channelExec.connect();
                InputStream in = channelExec.getInputStream();
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        System.out.print(new String(tmp, 0, i));
                    }
                    if (channelExec.isClosed()) {
                        if (in.available() > 0) continue;
                        System.out.println("Exit Status: " + channelExec.getExitStatus());
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }
                channelExec.disconnect();
            }
            if (args.scp && args.scpArgs.input != null && args.scpArgs.output != null) {
                // 使用 SCP 传输文件
                ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                channelSftp.put(args.scpArgs.input, args.scpArgs.output);
                channelSftp.disconnect();
            }
            // 关闭会话
            session.disconnect();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
