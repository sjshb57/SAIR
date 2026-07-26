package com.aefyr.pseudoapksigner;

import java.nio.charset.StandardCharsets;

class SignatureFileGenerator {

    private final ManifestBuilder mManifest;
    private final String mHashingAlgorithm;

    SignatureFileGenerator(ManifestBuilder manifestBuilder, String hashingAlgorithm) {
        mManifest = manifestBuilder;
        mHashingAlgorithm = hashingAlgorithm;
    }

    String generate() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(generateHeader());

        for (ManifestBuilder.ManifestEntry manifestEntry : mManifest.getEntries()) {
            ManifestBuilder.ManifestEntry sfEntry = new ManifestBuilder.ManifestEntry();
            sfEntry.setAttribute("Name", manifestEntry.getAttribute("Name"));
            sfEntry.setAttribute(mHashingAlgorithm + "-Digest", Utils.base64Encode(Utils.hash(manifestEntry.toString().getBytes(StandardCharsets.UTF_8), mHashingAlgorithm)));
            stringBuilder.append(sfEntry);
        }

        return stringBuilder.toString();
    }

    private ManifestBuilder.ManifestEntry generateHeader() throws Exception {
        ManifestBuilder.ManifestEntry header = new ManifestBuilder.ManifestEntry();
        header.setAttribute("Signature-Version", "1.0");
        header.setAttribute("Created-By", Constants.GENERATOR_NAME);
        header.setAttribute(mHashingAlgorithm + "-Digest-Manifest", Utils.base64Encode(Utils.hash(mManifest.build().getBytes(StandardCharsets.UTF_8), mHashingAlgorithm)));
        return header;
    }


}
