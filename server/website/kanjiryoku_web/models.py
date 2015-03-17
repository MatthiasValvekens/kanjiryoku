from django.db import models
from django.utils.translation import ugettext_lazy as _
from django.contrib.auth.models import User
from django.conf import settings
import bcrypt

USERNAME_MAX = getattr(settings, 'USERNAME_MAX', 10)

def gensalt_string():
	return bcrypt.gensalt().decode('ASCII')

class KanjiUser(models.Model):
	class Meta:
		verbose_name=_('Kanjiryoku user')
		verbose_name_plural=_('Kanjiryoku users')
	
	django_user = models.ForeignKey(User, verbose_name=_('Site user'))
	username = models.CharField(max_length=USERNAME_MAX, unique=True, verbose_name=_('Username'))
	created = models.DateTimeField(auto_now_add=True, verbose_name=_('Creation date'))
	modified = models.DateTimeField(auto_now=True, verbose_name=_('Last modified'))
	pwhash = models.CharField(max_length=60, verbose_name=_('Password'))
	salt = models.CharField(max_length=29, verbose_name=_('Salt'), editable=False, default=gensalt_string)
	admin = models.BooleanField(verbose_name=_('Administrator'),default=False)
	
	def save(self, *args, **kwargs):
		rawpass = self.pwhash
		salt_bin = self.salt.encode('ASCII')
		rawpass_bin = rawpass.encode('UTF-8')
		pwhash_bin = bcrypt.hashpw(rawpass_bin,salt_bin)
		self.pwhash = pwhash_bin.decode('ASCII')
		return super(KanjiUser, self).save(*args, **kwargs)
		