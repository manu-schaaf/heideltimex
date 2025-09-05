package de.unihd.dbs.heideltime.standalone.components.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.SofaID;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.Session;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Settings;

@SuppressWarnings("deprecation")
public class StandaloneConfigContext implements UimaContext {
	private HashMap<String, Object> settings = new HashMap<String, Object>();

	@Override
	public Object getConfigParameterValue(String aParamName) {
		return settings.get(aParamName);
	}

	public void setConfigParameterValue(String aParamName, Object aParamValue) {
		settings.put(aParamName, aParamValue);
	}

	@Override
	public Object getConfigParameterValue(String aGroupName, String aParamName) {
		return settings.get(aParamName);
	}
	
	/*
	 *  leave these defunct because we don't use them for now
	 */

	@Override
	public String[] getConfigurationGroupNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getConfigParameterNames(String aGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getConfigParameterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSharedSettingValue(String s) throws ResourceConfigurationException {
		throw new UnsupportedOperationException("getSharedSettingValue is not supported!");
	}

	@Override
	public String[] getSharedSettingArray(String s) throws ResourceConfigurationException {
		throw new UnsupportedOperationException("getSharedSettingArray is not supported!");
	}

	@Override
	public String[] getSharedSettingNames() {
		throw new UnsupportedOperationException("getSharedSettingNames is not supported!");
	}

	@Override
	public Settings getExternalOverrides() {
		throw new UnsupportedOperationException("getExternalOverrides is not supported!");
	}

	@Override
	public Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InstrumentationFacility getInstrumentationFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResourceURL(String aKey) throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getResourceURI(String aKey) throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResourceFilePath(String aKey)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String aKey)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getResourceObject(String aKey) throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResourceURL(String aKey, String[] aParams)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getResourceURI(String aKey, String[] aParams)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResourceFilePath(String aKey, String[] aParams)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String aKey, String[] aParams)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getResourceObject(String aKey, String[] aParams)
			throws ResourceAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDataPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDataPathElements() {
		return List.of();
	}

	@Override
	public Session getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SofaID mapToSofaID(String aSofaName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String mapSofaIDToComponentSofaName(String aSofaID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SofaID[] getSofaMappings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public AbstractCas getEmptyCas(Class aCasInterface) {
		// TODO Auto-generated method stub
		return null;
	}

}
