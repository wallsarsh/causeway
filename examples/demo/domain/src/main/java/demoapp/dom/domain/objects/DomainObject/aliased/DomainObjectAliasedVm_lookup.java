package demoapp.dom.domain.objects.DomainObject.aliased;

import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.services.bookmark.Bookmark;
import org.apache.causeway.applib.services.bookmark.BookmarkService;

//tag::class[]
@Action(semantics = SemanticsOf.SAFE)
@RequiredArgsConstructor
public class DomainObjectAliasedVm_lookup {

    @SuppressWarnings("unused")
    private final DomainObjectAliasedVm mixee;

    @MemberSupport
    public DomainObjectAliased act(final String bookmark) {
        return bookmarkService.lookup(Bookmark.parseElseFail(bookmark), DomainObjectAliased.class).orElseThrow(() -> new org.apache.causeway.applib.exceptions.RecoverableException("No customer exists for that bookmark"));
    }

    @Inject BookmarkService bookmarkService;
}
//end::class[]
