// Copyright 2015-2018 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import io.nats.client.AuthHandler;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

public class NatsReply {

    static final String usageString =
            "\nUsage: java NatsReply [server] <subject> <msgCount>"
            + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "Set the environment variable NATS_NKEY to use challenge resposne authentication by setting a file containing your seed.\n"
            + "Use the URL for user/pass/token authentication.\n";

    public static void main(String args[]) {
        String subject;
        int msgCount;
        String server;

        if (args.length == 3) {
            server = args[0];
            subject = args[1];
            msgCount = Integer.parseInt(args[2]);
        } else if (args.length == 2) {
            server = Options.DEFAULT_URL;
            subject = args[0];
            msgCount = Integer.parseInt(args[1]);
        } else {
            usage();
            return;
        }

        try {
            
            Options.Builder builder = new Options.Builder().server(server).noReconnect();

            if (System.getenv("NATS_NKEY") != null) {
                AuthHandler handler = new ExampleAuthHandler(System.getenv("NATS_NKEY"));
                builder.authHandler(handler);
            }

            Connection nc = Nats.connect(builder.build());
            CountDownLatch latch = new CountDownLatch(msgCount); // dispatcher runs callback in another thread
            
            Dispatcher d = nc.createDispatcher((msg) -> {
                System.out.printf("Received message \"%s\" on subject \"%s\", replying to %s\n", 
                                        new String(msg.getData(), StandardCharsets.UTF_8), 
                                        msg.getSubject(), msg.getReplyTo());
                nc.publish(msg.getReplyTo(), msg.getData());
                latch.countDown();
            });
            d.subscribe(subject);

            nc.flush(Duration.ZERO);

            latch.await();

            nc.closeDispatcher(d); // This isn't required, closing the connection will do it
            nc.close();
            
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    static void usage() {
        System.err.println(usageString);
        System.exit(-1);
    }
}