package gov.va.med.iss.meditor.core;

import gov.va.med.iss.meditor.resource.ResourceUtilsExtension;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class VFPackageRepo implements PackageRepository {
	private IFile packagesCSVFile;
	private Map<String, String> cache;
	
	public VFPackageRepo(IFile packagesCSVFile) {
		this.packagesCSVFile = packagesCSVFile;
	}
	
	private Map<String,String> loadPackages() {
		try {
			Map<String,String> results = new TreeMap<String, String>();
			IDocument document = ResourceUtilsExtension.getDocument(this.packagesCSVFile);
			int n = document.getNumberOfLines();
			String packageDirectory = null;
			for (int i=1; i<n; ++n) {
				IRegion info = document.getLineInformation(i);
				int length = info.getLength();
				if (length > 0) {
					int offset = info.getOffset();
					String line = document.get(offset, length); 
					String[] pieces = line.split(",");
					if (! pieces[0].isEmpty() && ! pieces[1].isEmpty() && ! pieces[2].isEmpty()) { //0 = package name, 1 - directory name, 2 - prefix
						packageDirectory = pieces[1];
						results.put(pieces[2], packageDirectory);
					} else if (! pieces[2].isEmpty()) {
						results.put(pieces[2], packageDirectory);
					}
				}
			}
			return results;
		} catch (Throwable t){
			throw new RuntimeException(t);
		}
	}
	
	@Override
	public String getPackageDirectory(String prefix) {
		if (this.cache == null) {
			cache = loadPackages();
		}
		return this.cache.get(prefix);
	}
}
