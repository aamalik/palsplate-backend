package com.jersey.resources;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jersey.persistence.PersonDao;
import com.jersey.representations.Person;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Transactional
@Component
public class PersonResource {

    private static final Logger log = LogManager.getLogger(PersonResource.class);

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private Cloudinary cloudinary;

    private PersonDao personDao;

    @Inject
    public PersonResource(PersonDao personDao) {
        this.personDao = personDao;
    }

    /**
     * Get all Users
     *
     * @return persons
     */
    @GET
    @Path("secure/persons")
   // @PreAuthorize("hasPermission('PersonResource', 'ROLE_ADMIN')")
    public List<Person> getAll() {
        List<Person> persons = this.personDao.findAll();
        return persons;
    }

    /**
     * Get single Person
     *
     * @param id
     * @return person
     */
    @GET
    @Path("secure/persons/{id}")
//    @PreAuthorize("hasPermission(#id, 'PersonResource', 'ROLE_USER,ROLE_ADMIN')")
    public Person getOne(@PathParam("id") long id) {
        Person person = personDao.findOne(id);
        if (person == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else {
            person.getPhotoName();
            person.getPhotoPublicId();
            return person;
        }
    }

    @GET
    @Path("secure/persons/currentuser")
    public Person getPersonViaAccessToken()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return CopyPersonSafe(personDao.findByEmail(email));
    }

    /**
     * Create new Person
     *
     * @param person
     * @return new person
     */
    @POST
    @Path("public/persons")
    public Person save(@Valid Person person) {

        person.setPassword(passwordEncoder.encode(person.getPassword()));

        return personDao.save(person);
    }

    /**
     * Create a Person Photo
     * @param uploadedInputStream
     * @param fileDetail
     * @param person_id
     * @return new Photo for a specific person
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("public/persons/{person_id}/photo")
    public Person uploadPhoto(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @PathParam("person_id")long person_id) throws IOException {

        log.info("fileDetail: " + fileDetail);
        log.info("fileDetail.getName: " + fileDetail.getName());
        log.info("fileDetail.getFileName: " + fileDetail.getFileName());

        Person person = personDao.findOne(person_id);

        if (person == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {

            File myfile = inputStream2file(uploadedInputStream, fileDetail.getFileName(), fileDetail.getType());
            Map uploadResult = cloudinary.uploader().upload(myfile, ObjectUtils.emptyMap());

            log.info("cloudinary secure_url: " + uploadResult.get("secure_url"));
            log.info("cloudinary public_id: " + uploadResult.get("public_id"));
            log.info("cloudinary original_filename: " + uploadResult.get("original_filename"));

            person.setPhotoName(fileDetail.getFileName());
            person.setPhotoPublicId(uploadResult.get("public_id").toString());

            return personDao.save(person);
        }
    }

    /**
     * Update existing Person
     *
     * @param id
     * @param person
     * @return updated person
     */
    @PUT
    @Path("secure/persons/{id}")
//    @PreAuthorize("hasPermission(#id, 'PersonResource', 'ROLE_USER,ROLE_ADMIN')")
    public Person update(@PathParam("id") long id, @Valid Person person) {
        if (personDao.findOne(id) == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else {
            person.setId(id);
            return personDao.save(person);
        }
    }

    /**
     * Delete person
     *
     * @param id
     */
    @DELETE
    @Path("secure/persons/{id}")
//    @PreAuthorize("hasPermission(#id, 'PersonResource', 'ROLE_ADMIN')")
    public void delete(@PathParam("id") long id) {
        Person person = personDao.findOne(id);
        if (person == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else {
            personDao.delete(person);
        }
    }

    /**
     * Copies the person object without password and other security impacting information
     * @return Copied person object
     */
    private Person CopyPersonSafe(Person person)
    {
        Person newPerson = new Person();

        newPerson.setEmail(person.getEmail());
        newPerson.setId(person.getId());
        newPerson.setFirstName(person.getFirstName());
        newPerson.setLastName(person.getLastName());
        newPerson.setAddress(person.getAddress());
        newPerson.setPhoneNumber(person.getPhoneNumber());
        newPerson.setDescription(person.getDescription());
        newPerson.setCook(person.getCook());
        newPerson.setCustomer(person.getCustomer());
        return  newPerson;
    }

    public File inputStream2file (InputStream in, String filename, String suffix) throws IOException {
        final File tempFile = File.createTempFile(filename, suffix);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }
}