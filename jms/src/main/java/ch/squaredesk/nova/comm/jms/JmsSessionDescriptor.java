/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

public class JmsSessionDescriptor {
    public final boolean transacted;
    public final int acknowledgeMode;

    public JmsSessionDescriptor(boolean transacted, int acknowledgeMode) {
        this.transacted = transacted;
        this.acknowledgeMode = acknowledgeMode;
    }

    @Override
    public String toString() {
        return "{transacted=" + transacted +
                ", acknowledgeMode=" + acknowledgeMode +
                '}';
    }
}
