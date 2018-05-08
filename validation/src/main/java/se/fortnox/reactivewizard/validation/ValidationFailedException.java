package se.fortnox.reactivewizard.validation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import se.fortnox.reactivewizard.jaxrs.WebException;

import io.netty.handler.codec.http.HttpResponseStatus;

@SuppressWarnings("serial")
public class ValidationFailedException extends WebException {

	private Set<ConstraintViolation<Object>> result;

	public ValidationFailedException(Set<ConstraintViolation<Object>> result) {
		super(HttpResponseStatus.BAD_REQUEST, ValidationFieldError.from(result));

		this.result = result;
	}

	private List<Map<String, String>> getUserError() {
		List<Map<String, String>> outp = new LinkedList<>();
		for (ConstraintViolation<Object> v : result) {
			Map<String, String> err = new HashMap<String, String>();
			err.put(v.getPropertyPath().toString(), v.getMessage());
			outp.add(err);
		}
		return outp;
	}

	@Override
	public String toString() {
		if (result != null) {
			return getUserError().toString();
		}
		return super.toString();
	}
}
