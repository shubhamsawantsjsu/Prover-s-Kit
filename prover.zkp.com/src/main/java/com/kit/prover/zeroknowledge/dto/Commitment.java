/*
 * Copyright 2017 ING Bank N.V.
 * This file is part of the go-ethereum library.
 *
 * The go-ethereum library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The go-ethereum library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the go-ethereum library. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.kit.prover.zeroknowledge.dto;

import java.io.Serializable;
import java.math.BigInteger;

public class Commitment implements Serializable {
    private SecretOrderGroup group;
    private BigInteger commitmentValue;

    public Commitment (SecretOrderGroup group, BigInteger commitmentValue) {
        this.group = group;
        this.commitmentValue = commitmentValue;
    }

    public SecretOrderGroup getGroup() {
        return group;
    }

    public BigInteger getCommitmentValue() {
        return commitmentValue;
    }
}
