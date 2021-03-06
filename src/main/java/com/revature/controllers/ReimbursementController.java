package com.revature.controllers;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.revature.annotations.Authorized;
import com.revature.models.Reimbursement;
import com.revature.models.UserRole;
import com.revature.services.ReimbursementService;
import com.revature.templates.ResolveTemplate;

@RestController
@RequestMapping("/api/v1/reimbursements")
public class ReimbursementController {

	@Autowired
	private ReimbursementService reimbursementService;

	@GetMapping
	public ResponseEntity<List<Reimbursement>> findAll() {
		return ResponseEntity.ok(this.reimbursementService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Reimbursement> findById(@PathVariable("id") int id) {
		return ResponseEntity.ok(this.reimbursementService.findById(id));
	}

	@PostMapping
	@Authorized(allowedRoles = { UserRole.Admin, UserRole.Employee })
	public ResponseEntity<Reimbursement> createReimbursement(@Valid @RequestBody Reimbursement reimbursement) {
		this.reimbursementService.insert(reimbursement);

		return ResponseEntity.created(URI.create(String.format("/reimbursements/%d", reimbursement.getId())))
				.body(reimbursement);
	}

	/**
	 * This request is leveraged to create a new Pending Reimbursement along with a
	 * receipt
	 * 
	 * @param reimbursement
	 * @param image
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Authorized(allowedRoles = { UserRole.Admin, UserRole.Employee })
	public ResponseEntity<Reimbursement> createReimbursementWithReceipt(@Valid @RequestPart Reimbursement reimbursement,
			@RequestPart MultipartFile image) {
		this.reimbursementService.attachReceipt(reimbursement, image);

		this.reimbursementService.insert(reimbursement);

		return ResponseEntity.created(URI.create(String.format("/reimbursements/%d", reimbursement.getId())))
				.body(reimbursement);
	}

	/**
	 * This request is leveraged to update a particular Reimbursement's details,
	 * such as amount or description.
	 * 
	 * If a Reimbursement needs to be resolved,
	 * {@link #resolveReimbursement(int, ResolveTemplate) resolveReimbursement}
	 * should be used instead.
	 */
	@PutMapping
	@Authorized(allowedRoles = { UserRole.Admin, UserRole.Employee })
	public ResponseEntity<Reimbursement> updateReimbursement(@Valid @RequestBody Reimbursement reimbursement) {
		return ResponseEntity.ok(this.reimbursementService.update(reimbursement));
	}

	/**
	 * This request is leveraged to resolve a Pending Reimbursement.
	 * 
	 * @param id       The id of the Reimbursement that is being resolved
	 * @param template Includes the status and resolver to finalize the
	 *                 reimbursement
	 */
	@PutMapping("/{id}")
	@Authorized(allowedRoles = UserRole.Admin)
	public ResponseEntity<Reimbursement> resolveReimbursement(@PathVariable("id") int id,
			@Valid @RequestBody ResolveTemplate template) {
		Reimbursement reimbursement = this.reimbursementService.findById(id);

		reimbursement.resolve(template.getStatus(), template.getResolver());

		return ResponseEntity.ok(this.reimbursementService.update(reimbursement));
	}

	/**
	 * This request is leveraged to attach an image of a receipt to a pre-existing
	 * Pending Reimbursement
	 * 
	 * @param id
	 * @param image
	 */
	@Authorized(allowedRoles = { UserRole.Admin, UserRole.Employee })
	@PostMapping(value = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Reimbursement> uploadReceipt(@PathVariable("id") int id, @RequestPart MultipartFile image) {
		Reimbursement reimbursement = this.reimbursementService.attachReceiptWithId(id, image);

		return ResponseEntity.ok(this.reimbursementService.update(reimbursement));
	}
}
