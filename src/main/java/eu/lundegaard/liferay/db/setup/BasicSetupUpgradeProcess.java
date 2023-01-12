/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Lundegaard a.s.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.lundegaard.liferay.db.setup;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Created by mapa on 13.3.2015.
 */
public abstract class BasicSetupUpgradeProcess extends UpgradeProcess {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactoryUtil.getLog(BasicSetupUpgradeProcess.class);

    /**

     This method is used to perform upgrade of the Liferay database, by reading and processing setup files.
     It first retrieves the names of the setup files using the {@link #getSetupFileNames()} method.
     Then, it iterates through the file names and for each file, it performs the following steps:
     <ul>
     <li>Opens an input stream to read the file from the classpath</li>
     <li>If the input stream is null, throws a {@link UpgradeException} with a message indicating that the XML configuration was not found</li>
     <li>Passes the input stream to the {@link LiferaySetup#setup(InputStream)} method to process the file</li>
     </ul>
     @throws UpgradeException if the XML configuration was not found
     */
    @Override
    public final void upgrade() throws UpgradeException {

        String[] fileNames = getSetupFileNames();
        for (String fileName : fileNames) {
            LOG.info("Starting upgrade process. Filename: " + fileName);

            InputStream is = BasicSetupUpgradeProcess.class.getClassLoader().getResourceAsStream(fileName);

            if (is == null) {
                throw new UpgradeException("XML configuration not found: " + fileName);
            }
            try {
                LiferaySetup.setup(is);
            } catch (FileNotFoundException | ParserConfigurationException | JAXBException | SAXException e) {
                e.printStackTrace();
            }
            LOG.info("Finished upgrade process. Filename: " + fileName);
        }
    }

    @Override
    protected void doUpgrade() throws Exception {
        this.upgrade();
    }

    /**
     * This method returns {@link String[]} with file names that are in the upgrade step.
     * @return paths to the setup xml files.
     */
    protected abstract String[] getSetupFileNames();
}
