from django.forms import ModelForm, PasswordInput
from kanjiryoku_web.models import KanjiUser

class KanjiryokuSignupForm(ModelForm):
	class Meta:
		model = KanjiUser
		widgets = {
			'pwhash': PasswordInput(),
		}
		exclude = ('django_user','admin')