/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 43 lines of code
 */
package cs352.RUBTClient.Group06;
import java.io.PrintWriter; 
import java.io.StringWriter; 
import java.util.Date;
import java.util.logging.Formatter; 
import java.util.logging.LogRecord; 
 
class CustomRecordFormatter extends Formatter { 
    @Override 
    public String format(final LogRecord r) { 
        StringBuilder sb = new StringBuilder(); 
        sb.append(RUBTConstants.date_format.format(new Date())).append(": ");
        sb.append(formatMessage(r)).append(System.getProperty("line.separator")); 
        if (null != r.getThrown()) { 
            sb.append("Throwable occurred: "); 
            Throwable t = r.getThrown(); 
            PrintWriter pw = null; 
            try { 
                StringWriter sw = new StringWriter(); 
                pw = new PrintWriter(sw); 
                t.printStackTrace(pw); 
                sb.append(sw.toString()); 
            } finally { 
                if (pw != null) { 
                    try { 
                        pw.close(); 
                    } catch (Exception e) { 
                        // ignore 
                    } 
                } 
            } 
        } 
        return sb.toString(); 
    } 
} 

