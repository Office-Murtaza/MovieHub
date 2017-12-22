package com.etiennelawlor.moviehub.domain;

import com.etiennelawlor.moviehub.data.repositories.person.PersonDataSourceContract;
import com.etiennelawlor.moviehub.data.repositories.person.models.PersonsPage;
import com.etiennelawlor.moviehub.util.EspressoIdlingResource;
import com.etiennelawlor.moviehub.util.rxjava.SchedulerTransformer2;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;

/**
 * Created by etiennelawlor on 6/26/17.
 */

public class PersonsUseCase implements PersonsDomainContract.UseCase {

    // region Member Variables
    private final PersonDataSourceContract.Repository personRepository;
    private final SchedulerTransformer2<PersonsPage> schedulerTransformer;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    // endregion

    // region Constructors
    public PersonsUseCase(PersonDataSourceContract.Repository personRepository, SchedulerTransformer2<PersonsPage> schedulerTransformer) {
        this.personRepository = personRepository;
        this.schedulerTransformer = schedulerTransformer;
    }
    // endregion

    // region PersonsDomainContract.UseCase Methods
    @Override
    public void clearSubscriptions() {
        if(compositeDisposable != null && compositeDisposable.isDisposed())
            compositeDisposable.clear();
    }

    @Override
    public void getPopularPersons(int currentPage, DisposableSingleObserver disposableSingleObserver) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        Disposable disposable = personRepository.getPopularPersons(currentPage)
                .compose(schedulerTransformer)
                .doOnSubscribe(disposable1 -> {
                    // The network request might be handled in a different thread so make sure Espresso knows
                    // that the app is busy until the response is handled.
                    EspressoIdlingResource.increment(); // App is busy until further notice
                })
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement(); // Set app as idle.
                    }
                })
                .subscribeWith(disposableSingleObserver);
        compositeDisposable.add(disposable);
    }
    // endregion

}
