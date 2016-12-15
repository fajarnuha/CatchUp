package io.sweers.catchup.rx.autodispose;

import io.reactivex.Maybe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.sweers.catchup.rx.autodispose.internal.AutoDisposableHelper;
import io.sweers.catchup.rx.autodispose.internal.AutoDisposeUtil;
import java.util.concurrent.atomic.AtomicReference;

public final class AutoDisposingObserver<T> implements Observer<T>, Disposable {

  private final AtomicReference<Disposable> mainDisposable = new AtomicReference<>();
  private final AtomicReference<Disposable> lifecycleDisposable = new AtomicReference<>();
  private final Maybe<?> lifecycle;
  private final Consumer<? super Throwable> onError;
  private final Consumer<? super T> onNext;
  private final Action onComplete;
  private final Consumer<? super Disposable> onSubscribe;

  AutoDisposingObserver(Maybe<?> lifecycle,
      Consumer<? super T> onNext,
      Consumer<? super Throwable> onError,
      Action onComplete,
      Consumer<? super Disposable> onSubscribe) {
    this.lifecycle = lifecycle;
    this.onNext = AutoDisposeUtil.emptyConsumerIfNull(onNext);
    this.onError = AutoDisposeUtil.emptyErrorConsumerIfNull(onError);
    this.onComplete = AutoDisposeUtil.emptyActionIfNull(onComplete);
    this.onSubscribe = AutoDisposeUtil.emptyDisposableIfNull(onSubscribe);
  }

  @Override
  public final void onSubscribe(Disposable d) {
    if (AutoDisposableHelper.setOnce(this.mainDisposable, d)) {
      AutoDisposableHelper.setOnce(this.lifecycleDisposable,
          lifecycle.subscribe(e -> dispose(), this::onError));
      try {
        onSubscribe.accept(this);
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        onError(t);
      }
    }
  }

  @Override
  public final boolean isDisposed() {
    return mainDisposable.get() == AutoDisposableHelper.DISPOSED;
  }

  @Override
  public final void dispose() {
    synchronized (this) {
      AutoDisposableHelper.dispose(lifecycleDisposable);
      AutoDisposableHelper.dispose(mainDisposable);
    }
  }

  @Override
  public final void onNext(T value) {
    if (!isDisposed()) {
      try {
        onNext.accept(value);
      } catch (Exception e) {
        Exceptions.throwIfFatal(e);
        onError(e);
      }
    }
  }

  @Override
  public final void onError(Throwable e) {
    if (!isDisposed()) {
      dispose();
      try {
        onError.accept(e);
      } catch (Exception e1) {
        Exceptions.throwIfFatal(e1);
        RxJavaPlugins.onError(new CompositeException(e, e1));
      }
    }
  }

  @Override
  public final void onComplete() {
    if (!isDisposed()) {
      dispose();
      try {
        onComplete.run();
      } catch (Exception e) {
        Exceptions.throwIfFatal(e);
        RxJavaPlugins.onError(e);
      }
    }
  }
}