package com.threadtrades.common;

import com.threadtrades.admin.CannotBanSelfException;
import com.threadtrades.admin.CannotDemoteSelfException;
import com.threadtrades.admin.InvalidSwapForceResolveException;
import com.threadtrades.auth.BannedUserException;
import com.threadtrades.auth.EmailAlreadyInUseException;
import com.threadtrades.auth.UsernameAlreadyInUseException;
import com.threadtrades.clothing.ClothingItemNotFoundException;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.message.MessageNotFoundException;
import com.threadtrades.review.ReviewNotFoundException;
import com.threadtrades.review.SwapNotCompletedException;
import com.threadtrades.storage.InvalidUploadException;
import com.threadtrades.storage.UnsupportedImageTypeException;
import com.threadtrades.swap.ExtraItemAlreadyAddedException;
import com.threadtrades.swap.ExtraItemAlreadyBaseItemException;
import com.threadtrades.swap.ExtraItemNoLongerAvailableException;
import com.threadtrades.swap.ExtraItemNotAvailableException;
import com.threadtrades.swap.ExtraItemNotFoundException;
import com.threadtrades.swap.ExtraItemNotOwnedException;
import com.threadtrades.swap.SwapAlreadyDecidedException;
import com.threadtrades.swap.SwapNotFoundException;
import com.threadtrades.swipe.CannotSwipeOwnItemException;
import com.threadtrades.swipe.ItemAlreadySwipedException;
import com.threadtrades.swipe.OfferedItemNotOwnedException;
import com.threadtrades.user.UserProfileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    // Bean-validation annotations (@NotBlank, @Size, ...) on @RequestParam/@RequestPart method
    // parameters are enforced by the @Validated AOP proxy, which throws this instead of
    // HandlerMethodValidationException above -- both are "a request parameter failed validation",
    // just raised through different mechanisms depending on where in the MVC pipeline Spring
    // catches it.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ApiError("Uploaded file is too large"));
    }

    @ExceptionHandler(InvalidUploadException.class)
    public ResponseEntity<ApiError> handleInvalidUpload(InvalidUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedImageTypeException.class)
    public ResponseEntity<ApiError> handleUnsupportedImageType(UnsupportedImageTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({
        ClothingItemNotFoundException.class,
        MatchNotFoundException.class,
        UserProfileNotFoundException.class,
        SwapNotFoundException.class,
        MessageNotFoundException.class,
        ReviewNotFoundException.class,
        ExtraItemNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({
        EmailAlreadyInUseException.class,
        UsernameAlreadyInUseException.class,
        ItemAlreadySwipedException.class,
        SwapAlreadyDecidedException.class,
        SwapNotCompletedException.class,
        ExtraItemAlreadyAddedException.class,
        ExtraItemNoLongerAvailableException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({
        ExtraItemNotOwnedException.class,
        ExtraItemNotAvailableException.class,
        ExtraItemAlreadyBaseItemException.class
    })
    public ResponseEntity<ApiError> handleExtraItemBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(CannotSwipeOwnItemException.class)
    public ResponseEntity<ApiError> handleCannotSwipeOwnItem(CannotSwipeOwnItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(OfferedItemNotOwnedException.class)
    public ResponseEntity<ApiError> handleOfferedItemNotOwned(OfferedItemNotOwnedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(InvalidSwapForceResolveException.class)
    public ResponseEntity<ApiError> handleInvalidSwapForceResolve(InvalidSwapForceResolveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(CannotBanSelfException.class)
    public ResponseEntity<ApiError> handleCannotBanSelf(CannotBanSelfException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(CannotDemoteSelfException.class)
    public ResponseEntity<ApiError> handleCannotDemoteSelf(CannotDemoteSelfException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<ApiError> handleBannedUser(BannedUserException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage()));
    }
}
