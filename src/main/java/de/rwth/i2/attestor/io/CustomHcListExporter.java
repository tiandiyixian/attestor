package de.rwth.i2.attestor.io;

import java.io.IOException;
import java.util.List;

import de.rwth.i2.attestor.io.jsonImport.HcLabelPair;

/**
 * Created by christina on 23.08.17.
 */
public interface CustomHcListExporter {

    void export(String directory, List<HcLabelPair> hcList) throws IOException;
}
