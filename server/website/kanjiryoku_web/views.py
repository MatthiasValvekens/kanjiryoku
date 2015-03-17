from django.views.generic.edit import CreateView
from kanjiryoku_web.models import KanjiUser
from kanjiryoku_web.forms import KanjiryokuSignupForm
from django.core.urlresolvers import reverse
from django.shortcuts import render_to_response

class SignupView(CreateView):
	model = KanjiUser
	form_class = KanjiryokuSignupForm
	def get_success_url(self):
		return reverse('kanjiryoku_signup_success') 	
	def form_valid(self, form):
		form.instance.django_user = self.request.user
		return super(SignupView, self).form_valid(form)

def success(request):
	return render_to_response('kanjiryoku_web/success.html')