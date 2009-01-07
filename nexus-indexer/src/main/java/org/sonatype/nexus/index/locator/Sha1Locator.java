/**
 * Copyright © 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 * Eugene Kuleshov (Sonatype)
 * Tamas Cservenak (Sonatype)
 * Brian Fox (Sonatype)
 * Jason Van Zyl (Sonatype)
 */
package org.sonatype.nexus.index.locator;

import java.io.File;

/** @author Jason van Zyl */
public class Sha1Locator
    implements Locator
{
    public File locate( File source )
    {
        // return new File( source.getParentFile(), source.getName() + ".sha1" );
        return new File( source.getAbsolutePath() + ".sha1" );
    }
}
