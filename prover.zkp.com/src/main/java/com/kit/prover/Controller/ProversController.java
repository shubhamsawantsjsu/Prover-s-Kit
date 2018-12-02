package com.kit.prover.Controller;

import com.kit.prover.Amazon_S3.Download;
import com.kit.prover.Amazon_S3.Upload;
import com.kit.prover.Helpers.ParseBalanceHelper;
import com.kit.prover.Helpers.ParseBirthCerti;
import com.kit.prover.zeroknowledge.RangeProof;
import com.kit.prover.zeroknowledge.dto.BoudotRangeProof;
import com.kit.prover.zeroknowledge.dto.ClosedRange;
import com.kit.prover.zeroknowledge.dto.Commitment;
import com.kit.prover.zeroknowledge.dto.TTPMessage;
import com.kit.prover.zeroknowledge.prover.Config;
import com.kit.prover.zeroknowledge.prover.TTPDemo;
import com.kit.prover.zeroknowledge.prover.Verifier;
import com.kit.prover.zeroknowledge.util.InputUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;

@Controller
public class ProversController {

    @Autowired
    private UserRepository userRepository;

    String downloadLocation = Config.getInstance().getProperty("download.location");

    @GetMapping(path="/user/{id}")
    public @ResponseBody String index(@PathVariable("id") String uid) throws ParseException {

        System.out.println("unique_doc_id is: "+uid);

        Iterable<Zkp_user> provers = userRepository.findAll();

        Zkp_user prover = null;
        for (Zkp_user p: provers) {
            System.out.println(p.getDoc_name());
            if(p.getUnique_doc_id().equalsIgnoreCase(uid)) {
                prover = p;
                break;
            }
        }

        System.out.println("The document name is: "+ prover.getDoc_name());

        try {
            Download download = new Download();
            download.getTheFile(prover.getDoc_name());
        }
        catch (IOException e) {
            System.out.println("Error occured while fetching the file from Amazon S3");
        }

        StringBuilder sb = new StringBuilder();
        try {
            PDDocument document = PDDocument.load(new File(downloadLocation + prover.getDoc_name()));
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                sb.append(text);
            }
            document.close();
        }
        catch(Exception e) {
            System.out.println("File is either not present or is corrupted");
        }

        System.out.println(sb.toString());

        if(Config.getInstance().getProperty("balance_proof").equalsIgnoreCase(prover.getDoc_type())) {
            ParseBalanceHelper parseBalanceHelper = new ParseBalanceHelper();
            String balance = parseBalanceHelper.getBalanceFromConfirmationLetter(sb.toString());

            new TTPDemo().generateTrustedMessage(new BigInteger(balance), prover.getDoc_name());
        }
        else if(Config.getInstance().getProperty("age_proof").equalsIgnoreCase(prover.getDoc_type())) {
            ParseBirthCerti parseBirthCerti = new ParseBirthCerti();
            String age = parseBirthCerti.getAgeFromBirthDate(sb.toString());

            new TTPDemo().generateTrustedMessage(new BigInteger(age), prover.getDoc_name());
        }

        try {
            Upload upload = new Upload();
            upload.uploadTheFile(prover.getDoc_name());
        }
        catch (IOException e) {
            System.out.println("Error occurred while uploading the file to S3.");
        }
        System.out.println("Generated the proof successfully!!");
        return "Range proof has been generated successfully";
    }


    //Just to add a dummy value in the table
    @GetMapping(path="/add")
    public @ResponseBody String addNewUser () {

        Zkp_user prover = new Zkp_user();
        prover.setUsername("shubham.sawant@sjsu.edu");
        prover.setPassword("jsjh#hbsdh");
        prover.setDoc_type("balance_proof");
        prover.setUnique_doc_id("234567");
        prover.setType("prover");
        prover.setDoc_name("234567_balance_proof_timestamp.pdf");
        userRepository.save(prover);
        return "Saved";
    }

    @PostMapping("/verifier")
    public @ResponseBody String index(@RequestBody InputBody inputBody) {

        String uid = inputBody.uid;
        String lowerBound = inputBody.lowerBound;
        String upperBound = inputBody.upperBound;

        System.out.println("Unique_doc_id is: "+uid);

        Iterable<Zkp_user> zkp_users = userRepository.findAll();

        Zkp_user zkp_user = null;
        for (Zkp_user p: zkp_users) {
            if(uid.equalsIgnoreCase(p.getUnique_doc_id())
                    && p.getType().equalsIgnoreCase("Prover")) {
                zkp_user = p;
                break;
            }
        }

        if (zkp_user==null) return "User not found!!!";

        String fileName = zkp_user.getDoc_name() + ".data";
        /*try {
            Download download = new Download();
            download.getTheFile(zkp_user.getDoc_name() + ".data");
        }
        catch (IOException e) {
            System.out.println("Error occurred while uploading the file to S3.");
        }*/

        //Verifier verifier = new Verifier()
        //verifier.runValidation(lowerBound, upperBound, fileName)

        Verifier verifier = new Verifier();
        if (verifier.runValidation(lowerBound, upperBound, fileName)) {
            System.out.println("Generated the proof successfully!!");
            return "success";
        }

        return "error";
    }

    public boolean runValidation(String lowerBound, String upperBound, String fileName) {

        ClosedRange range = getRange(lowerBound, upperBound);

        System.out.println("Reading commitment from trusted 3rd party");

        String fileLocation = Config.getInstance().getProperty("upload.location");

        System.out.println("Filename is: "+ fileLocation + fileName);

        TTPMessage ttpMessage = (TTPMessage) InputUtils.readObject(fileLocation+fileName);
        Commitment commitment = ttpMessage.getCommitment();

        System.out.println("the value of x is: "+ttpMessage.getX());
        System.out.println("Range is: "+range);
        BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(ttpMessage, range);

        return validateJava(rangeProof, commitment, range);
    }

    private boolean validateJava(BoudotRangeProof rangeProof, Commitment commitment, ClosedRange range) {

        try {
            if(RangeProof.validateRangeProof(rangeProof, commitment, range)) {
                return true;
            }
            else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static ClosedRange getRange(String lowerBound, String upperBound) {

        BigInteger lo, hi;

        lo = new BigInteger(lowerBound);
        hi = new BigInteger(upperBound);

        return ClosedRange.of(lo, hi);
    }
}
