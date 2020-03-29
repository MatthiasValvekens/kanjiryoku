from django.conf.urls import patterns, include, url
import kanjiryoku_web.views as views
from django.contrib.auth.decorators import login_required

urlpatterns = patterns('',
    url(r'^signup/$', login_required(views.SignupView.as_view()),name='kanjiryoku_signup'),
    url(r'^success/$', login_required(views.success),name='kanjiryoku_signup_success'),
)