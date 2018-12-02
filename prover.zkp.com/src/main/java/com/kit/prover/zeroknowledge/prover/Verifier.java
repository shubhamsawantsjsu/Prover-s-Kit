package com.kit.prover.zeroknowledge.prover;


import com.kit.prover.zeroknowledge.RangeProof;
import com.kit.prover.zeroknowledge.dto.BoudotRangeProof;
import com.kit.prover.zeroknowledge.dto.ClosedRange;
import com.kit.prover.zeroknowledge.dto.Commitment;
import com.kit.prover.zeroknowledge.dto.TTPMessage;
import com.kit.prover.zeroknowledge.util.ExportUtil;
import com.kit.prover.zeroknowledge.util.InputUtils;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;

public class Verifier {

    public boolean runValidation(String lowerBound, String upperBound, String fileName) {

        ClosedRange range = getRange(lowerBound, upperBound);

        System.out.println("Reading commitment from trusted 3rd party");

        String fileLocation = Config.getInstance().getProperty("upload.location");

        System.out.println("Filename is: "+ fileLocation + fileName);

        TTPMessage ttpMessage = (TTPMessage) InputUtils.readObject(fileLocation+fileName);

        //TTPMessage ttpMessage = (TTPMessage) InputUtils.readObject(fileName);
        Commitment commitment = ttpMessage.getCommitment();

        System.out.println("the value of x is: "+ttpMessage.getX());
        System.out.println("Range is: "+range);

        /*if (!range.contains(ttpMessage.getX())) {
            throw new IllegalArgumentException("Provided range does not contain the committed value");
        }*/

        BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(ttpMessage, range);
        //InputUtils.saveObject("src/main/resources/range-proof.data", rangeProof);
        //BoudotRangeProof rangeProof = (BoudotRangeProof)InputUtils.readObject("src/main/resources/range-proof.data");

        System.out.println("Commitment = ");
        System.out.println(DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(commitment)));

        System.out.println("Proof = ");
        System.out.println(DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(rangeProof, commitment, range)));

        return validateJava(rangeProof, commitment, range);
    }

    private static ClosedRange getRange(String lowerBound, String upperBound) {

        BigInteger lo, hi;

        lo = new BigInteger(lowerBound);
        hi = new BigInteger(upperBound);

        return ClosedRange.of(lo, hi);
    }

    private boolean validateJava(BoudotRangeProof rangeProof, Commitment commitment, ClosedRange range) {

        try {
            if(RangeProof.validateRangeProof(rangeProof, commitment, range)) {
                System.out.println("Range proof validated successfully");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Range proof validation error: " + e.getMessage());
            return false;
        }
        return false;
    }
}