package org.openmrs.module.ebolaexample.rest.controller;

import junit.framework.Assert;
import org.apache.commons.lang.time.DateUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.ebolaexample.EbolaRestTestBase;
import org.openmrs.module.ebolaexample.metadata.EbolaMetadata;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class VitalsAndSymptomsObservationControllerTest extends EbolaRestTestBase {

    private String requestURI = "/rest/" + RestConstants.VERSION_1 + "/ebola/encounter/vitals-and-symptoms";

    @Test
    public void shouldGetEmptyListIfEncounterNotExisted() throws Exception{
        Patient patient = Context.getPatientService().getPatient(2);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.addHeader("content-type", "application/json");
        request.addParameter("patientUuid", patient.getUuid());
        request.addParameter("formUuid", EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS);
        request.addParameter("top", "3");

        MockHttpServletResponse response = webMethods.handle(request);
        SimpleObject responseObject = new ObjectMapper().readValue(response.getContentAsString(), SimpleObject.class);
        ArrayList<LinkedHashMap> encounters = (ArrayList<LinkedHashMap>)responseObject.get("encounters");

        Assert.assertEquals(0, encounters.size());
    }

    @Test
    public void shouldOnlyGetSymptomsObs() throws Exception {
        Patient patient = Context.getPatientService().getPatient(2);

        Date yesterday = DateUtils.addDays(new Date(), -1);
        HashSet<Obs> obses = new HashSet<Obs>();
        obses.add(getNumericObs(patient, yesterday));
        new EbolaEncounterBuilder().createEncounter(patient, EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS, yesterday, obses, EbolaMetadata._EncounterType.EBOLA_INPATIENT_FOLLOWUP);

        Date today = new Date();
        HashSet<Obs> obses1 = new HashSet<Obs>();
        obses1.add(getNumericObs(patient, today));
        obses1.add(getCodedObs(patient, today));
        new EbolaEncounterBuilder().createEncounter(patient, EbolaMetadata._Form.EBOLA_VITALS_FORM, today, obses1, EbolaMetadata._EncounterType.EBOLA_INPATIENT_FOLLOWUP);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.addHeader("content-type", "application/json");
        request.addParameter("patientUuid", patient.getUuid());
        request.addParameter("formUuid", EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS);
        request.addParameter("top", "3");

        MockHttpServletResponse response = webMethods.handle(request);
        SimpleObject responseObject = new ObjectMapper().readValue(response.getContentAsString(), SimpleObject.class);
        ArrayList<LinkedHashMap> encounters = (ArrayList<LinkedHashMap>)responseObject.get("encounters");

        Assert.assertEquals(1, encounters.size());
    }

    @Test
    public void shouldGetResultForObsWithNumericAndCodedConcepts() throws Exception{
        Patient patient = Context.getPatientService().getPatient(2);

        Date today = new Date();
        HashSet<Obs> obses1 = new HashSet<Obs>();
        Obs numericObs = getNumericObs(patient, today);
        obses1.add(numericObs);
        Obs codedObs = getCodedObs(patient, today);
        obses1.add(codedObs);
        new EbolaEncounterBuilder().createEncounter(patient, EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS, today, obses1, EbolaMetadata._EncounterType.EBOLA_INPATIENT_FOLLOWUP);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.addHeader("content-type", "application/json");
        request.addParameter("patientUuid", patient.getUuid());
        request.addParameter("formUuid", EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS);

        MockHttpServletResponse response = webMethods.handle(request);
        SimpleObject responseObject = new ObjectMapper().readValue(response.getContentAsString(), SimpleObject.class);
        ArrayList<LinkedHashMap> encounters = (ArrayList<LinkedHashMap>)responseObject.get("encounters");
        ArrayList<LinkedHashMap> obs = (ArrayList<LinkedHashMap>)encounters.get(0).get("obs");
        Assert.assertEquals(1, encounters.size());
        Assert.assertEquals(2, obs.size());


        for(LinkedHashMap map : obs){
            String concept = (String)map.get("concept");
            if(!concept.equals(numericObs.getConcept().getUuid())){
                Assert.assertEquals(codedObs.getValueCoded().getUuid(), map.get("value"));
            }
            else{
                Assert.assertEquals(numericObs.getValueNumeric(), map.get("value"));
            }
        }
    }

    @Test
    public void shouldGetAllSymptomsObs() throws Exception{
        Patient patient = Context.getPatientService().getPatient(2);

        Date threeDaysAgo = DateUtils.addDays(new Date(), -3);
        Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
        Date yesterday = DateUtils.addDays(new Date(), -1);
        Date today = DateUtils.addDays(new Date(), 0);

        createSymptomEncounter(patient, threeDaysAgo);
        createSymptomEncounter(patient, twoDaysAgo);
        createSymptomEncounter(patient, yesterday);
        createSymptomEncounter(patient, today);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.addHeader("content-type", "application/json");
        request.addParameter("patientUuid", patient.getUuid());
        request.addParameter("formUuid", EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS);

        MockHttpServletResponse response = webMethods.handle(request);
        SimpleObject responseObject = new ObjectMapper().readValue(response.getContentAsString(), SimpleObject.class);
        ArrayList<LinkedHashMap> encounters = (ArrayList<LinkedHashMap>)responseObject.get("encounters");
        Assert.assertEquals(4, encounters.size());
        Assert.assertEquals(today.getTime()/1000, (Long) encounters.get(0).get("dateCreated") /1000);
        Assert.assertEquals(yesterday.getTime()/1000, (Long) encounters.get(1).get("dateCreated") /1000);
        Assert.assertEquals(twoDaysAgo.getTime()/1000, (Long) encounters.get(2).get("dateCreated") /1000);
        Assert.assertEquals(threeDaysAgo.getTime()/1000, (Long) encounters.get(3).get("dateCreated") /1000);
    }

    @Test
    public void shouldGetLatest3SymptomsObs() throws Exception{
        Patient patient = Context.getPatientService().getPatient(2);

        Date threeDaysAgo = DateUtils.addDays(new Date(), -3);
        Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
        Date yesterday = DateUtils.addDays(new Date(), -1);
        Date today = DateUtils.addDays(new Date(), 0);

        createSymptomEncounter(patient, threeDaysAgo);
        createSymptomEncounter(patient, twoDaysAgo);
        createSymptomEncounter(patient, yesterday);
        createSymptomEncounter(patient, today);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.addHeader("content-type", "application/json");
        request.addParameter("patientUuid", patient.getUuid());
        request.addParameter("formUuid", EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS);
        request.addParameter("top", "3");

        MockHttpServletResponse response = webMethods.handle(request);
        SimpleObject responseObject = new ObjectMapper().readValue(response.getContentAsString(), SimpleObject.class);
        ArrayList<LinkedHashMap> encounters = (ArrayList<LinkedHashMap>)responseObject.get("encounters");
        Assert.assertEquals(3, encounters.size());
        Assert.assertEquals(today.getTime()/1000, (Long) encounters.get(0).get("dateCreated") /1000);
        Assert.assertEquals(yesterday.getTime()/1000, (Long) encounters.get(1).get("dateCreated") /1000);
        Assert.assertEquals(twoDaysAgo.getTime()/1000, (Long) encounters.get(2).get("dateCreated") /1000);
    }

    private void createSymptomEncounter(Patient patient, Date dateCreated) {
        HashSet<Obs> obses = new HashSet<Obs>();
        Obs codedObs = getCodedObs(patient, dateCreated);
        obses.add(codedObs);
        new EbolaEncounterBuilder().createEncounter(patient, EbolaMetadata._Form.EBOLA_CLINICAL_SIGNS_AND_SYMPTOMS, dateCreated, obses, EbolaMetadata._EncounterType.EBOLA_INPATIENT_FOLLOWUP);
    }

    private Obs getCodedObs(Patient patient, Date dateCreated) {
        Concept concept = Context.getConceptService().getConcept(21);
        Obs obs = new Obs(patient, concept, dateCreated, null);
        Concept answer_concept = Context.getConceptService().getConcept(7);
        obs.setValueCoded(answer_concept);
        return obs;
    }

    private Obs getNumericObs(Patient patient, Date dateCreated) {
        Concept concept = Context.getConceptService().getConcept(5089);
        Obs obs = new Obs(patient, concept, dateCreated, null);
        obs.setValueNumeric(1.0);
        return obs;
    }
}